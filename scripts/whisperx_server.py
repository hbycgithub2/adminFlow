#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
WhisperX HTTP服务（常驻进程模式）
用途：提供HTTP API接口，实现模型常驻内存，避免每次重新加载
性能：首次加载30秒，后续每次对齐1-3秒（提升10-20倍）

作者：Kiro AI Assistant
日期：2026-08-16
版本：1.0
"""

# 设置环境变量（必须在任何import之前）
import os
import sys

# SSL修复
import ssl
ssl._create_default_https_context = ssl._create_unverified_context
os.environ['CURL_CA_BUNDLE'] = ''
os.environ['REQUESTS_CA_BUNDLE'] = ''
os.environ['PYTHONHTTPSVERIFY'] = '0'
os.environ['HTTPX_VERIFY'] = 'false'
os.environ['HF_HUB_DISABLE_SSL_VERIFY'] = '1'
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

# FFmpeg路径
os.environ['PATH'] = r'D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin' + os.pathsep + os.environ.get('PATH', '')

# Flask和基础库
from flask import Flask, request, jsonify
import whisperx
import torch
import logging
import traceback
from pathlib import Path

# 禁用Flask的默认日志
log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

# 创建Flask应用
app = Flask(__name__)

# 全局变量（模型缓存）
whisper_model = None
align_models = {}  # 缓存不同语言的对齐模型
device = "cuda" if torch.cuda.is_available() else "cpu"
compute_type = "float32"

print(f"[WhisperX Server] 使用设备：{device}", flush=True)

def init_models():
    """
    初始化模型（启动时加载，常驻内存）
    """
    global whisper_model
    
    try:
        print(f"[WhisperX Server] 正在加载Whisper base模型...", flush=True)
        whisper_model = whisperx.load_model("base", device=device, compute_type=compute_type)
        print(f"[WhisperX Server] [OK] Whisper模型加载完成", flush=True)
    except Exception as e:
        print(f"[WhisperX Server] [ERROR] 模型加载失败：{e}", flush=True)
        raise

def get_align_model(language):
    """
    获取对齐模型（懒加载+缓存）
    """
    global align_models
    
    if language not in align_models:
        print(f"[WhisperX Server] 加载{language}对齐模型...", flush=True)
        try:
            align_model, metadata = whisperx.load_align_model(language_code=language, device=device)
            align_models[language] = (align_model, metadata)
            print(f"[WhisperX Server] [OK] {language}对齐模型加载完成", flush=True)
        except Exception as e:
            print(f"[WhisperX Server] [ERROR] {language}对齐模型加载失败：{e}", flush=True)
            raise
    
    return align_models[language]

@app.route('/health', methods=['GET'])
def health():
    """
    健康检查接口
    """
    return jsonify({
        "status": "healthy",
        "device": device,
        "model_loaded": whisper_model is not None
    })

@app.route('/align', methods=['POST'])
def align():
    """
    单个音频对齐接口
    
    请求格式：
    {
        "audio": "/path/to/audio.mp3",
        "text": "原文文本"
    }
    
    返回格式：
    {
        "success": true,
        "characters": [
            {"char": "你", "start": 0.0, "end": 0.5},
            ...
        ],
        "audio_duration": 10.5,
        "audio_offset": 0.0
    }
    """
    try:
        data = request.get_json()
        audio_path = data.get('audio')
        original_text = data.get('text')
        language = data.get('language', 'auto')
        
        if not audio_path or not original_text:
            return jsonify({
                "success": False,
                "error": "缺少参数：audio或text"
            }), 400
        
        # 检查文件是否存在
        if not Path(audio_path).exists():
            return jsonify({
                "success": False,
                "error": f"音频文件不存在：{audio_path}"
            }), 400
        
        print(f"[WhisperX Server] 开始对齐，音频：{audio_path}，文本长度：{len(original_text)}", flush=True)
        
        # 自动检测语言
        if language == 'auto':
            chinese_chars = sum(1 for c in original_text if '\u4e00' <= c <= '\u9fff')
            language = 'zh' if chinese_chars / len(original_text) > 0.3 else 'en'
            print(f"[WhisperX Server] 自动检测语言：{language}", flush=True)
        
        # 加载音频
        audio = whisperx.load_audio(audio_path)
        
        # Whisper粗略识别（用于分段）
        result = whisper_model.transcribe(audio, language=language)
        
        # 加载对齐模型
        align_model, metadata = get_align_model(language)
        
        # 替换为原文
        if result["segments"]:
            result["segments"] = [{
                "start": result["segments"][0]["start"],
                "end": result["segments"][-1]["end"],
                "text": original_text
            }]
        
        # 执行对齐
        aligned_result = whisperx.align(
            result["segments"],
            align_model,
            metadata,
            audio,
            device=device,
            return_char_alignments=True
        )
        
        # 提取字符级时间戳
        char_timings = []
        audio_start_offset = 0.0
        first_char_found = False
        
        for segment in aligned_result["segments"]:
            if "chars" in segment and segment["chars"]:
                for char_info in segment["chars"]:
                    if not first_char_found:
                        audio_start_offset = char_info["start"]
                        first_char_found = True
                    
                    char_timings.append({
                        "char": char_info["char"],
                        "start": round(char_info["start"] - audio_start_offset, 3),
                        "end": round(char_info["end"] - audio_start_offset, 3)
                    })
            elif "words" in segment and segment["words"]:
                for word_info in segment["words"]:
                    word_text = word_info["word"]
                    word_start = word_info["start"]
                    word_end = word_info["end"]
                    word_duration = word_end - word_start
                    
                    if not first_char_found:
                        audio_start_offset = word_start
                        first_char_found = True
                    
                    char_count = len(word_text)
                    char_duration = word_duration / char_count if char_count > 0 else 0
                    
                    for i, char in enumerate(word_text):
                        char_start = word_start + i * char_duration - audio_start_offset
                        char_timings.append({
                            "char": char,
                            "start": round(char_start, 3),
                            "end": round(char_start + char_duration, 3)
                        })
        
        audio_duration = char_timings[-1]["end"] if char_timings else 0.0
        
        print(f"[WhisperX Server] [OK] 对齐完成，字符数：{len(char_timings)}", flush=True)
        
        return jsonify({
            "success": True,
            "characters": char_timings,
            "audio_duration": audio_duration,
            "audio_offset": audio_start_offset
        })
        
    except Exception as e:
        error_detail = traceback.format_exc()
        print(f"[WhisperX Server] [ERROR] 对齐失败：{error_detail}", flush=True)
        return jsonify({
            "success": False,
            "error": str(e),
            "error_detail": error_detail
        }), 500

@app.route('/align_batch', methods=['POST'])
def align_batch():
    """
    批量对齐接口
    
    请求格式：
    {
        "requests": [
            {"audio": "/path/to/audio1.mp3", "text": "文本1"},
            {"audio": "/path/to/audio2.mp3", "text": "文本2"}
        ]
    }
    
    返回格式：
    {
        "success": true,
        "results": [
            {"success": true, "characters": [...]},
            {"success": true, "characters": [...]}
        ]
    }
    """
    try:
        data = request.get_json()
        requests_list = data.get('requests', [])
        
        if not requests_list:
            return jsonify({
                "success": False,
                "error": "requests列表为空"
            }), 400
        
        print(f"[WhisperX Server] 开始批量对齐，数量：{len(requests_list)}", flush=True)
        
        results = []
        
        for i, req in enumerate(requests_list):
            try:
                audio_path = req.get('audio')
                original_text = req.get('text')
                language = req.get('language', 'auto')
                
                # 重用单个对齐逻辑
                if not Path(audio_path).exists():
                    results.append({
                        "success": False,
                        "error": f"文件不存在：{audio_path}"
                    })
                    continue
                
                # 自动检测语言
                if language == 'auto':
                    chinese_chars = sum(1 for c in original_text if '\u4e00' <= c <= '\u9fff')
                    language = 'zh' if chinese_chars / len(original_text) > 0.3 else 'en'
                
                # 加载音频
                audio = whisperx.load_audio(audio_path)
                
                # Whisper粗略识别
                result = whisper_model.transcribe(audio, language=language)
                
                # 加载对齐模型
                align_model, metadata = get_align_model(language)
                
                # 替换为原文
                if result["segments"]:
                    result["segments"] = [{
                        "start": result["segments"][0]["start"],
                        "end": result["segments"][-1]["end"],
                        "text": original_text
                    }]
                
                # 执行对齐
                aligned_result = whisperx.align(
                    result["segments"],
                    align_model,
                    metadata,
                    audio,
                    device=device,
                    return_char_alignments=True
                )
                
                # 提取字符级时间戳
                char_timings = []
                audio_start_offset = 0.0
                first_char_found = False
                
                for segment in aligned_result["segments"]:
                    if "chars" in segment and segment["chars"]:
                        for char_info in segment["chars"]:
                            if not first_char_found:
                                audio_start_offset = char_info["start"]
                                first_char_found = True
                            
                            char_timings.append({
                                "char": char_info["char"],
                                "start": round(char_info["start"] - audio_start_offset, 3),
                                "end": round(char_info["end"] - audio_start_offset, 3)
                            })
                    elif "words" in segment and segment["words"]:
                        for word_info in segment["words"]:
                            word_text = word_info["word"]
                            word_start = word_info["start"]
                            word_end = word_info["end"]
                            word_duration = word_end - word_start
                            
                            if not first_char_found:
                                audio_start_offset = word_start
                                first_char_found = True
                            
                            char_count = len(word_text)
                            char_duration = word_duration / char_count if char_count > 0 else 0
                            
                            for j, char in enumerate(word_text):
                                char_start = word_start + j * char_duration - audio_start_offset
                                char_timings.append({
                                    "char": char,
                                    "start": round(char_start, 3),
                                    "end": round(char_start + char_duration, 3)
                                })
                
                results.append({
                    "success": True,
                    "characters": char_timings
                })
                
                print(f"[WhisperX Server] [OK] 第{i+1}/{len(requests_list)}个完成", flush=True)
                
            except Exception as e:
                error_detail = traceback.format_exc()
                print(f"[WhisperX Server] [ERROR] 第{i+1}个失败：{error_detail}", flush=True)
                results.append({
                    "success": False,
                    "error": str(e)
                })
        
        print(f"[WhisperX Server] [OK] 批量对齐完成", flush=True)
        
        return jsonify({
            "success": True,
            "results": results
        })
        
    except Exception as e:
        error_detail = traceback.format_exc()
        print(f"[WhisperX Server] [ERROR] 批量对齐失败：{error_detail}", flush=True)
        return jsonify({
            "success": False,
            "error": str(e),
            "error_detail": error_detail
        }), 500

if __name__ == '__main__':
    print("=" * 80, flush=True)
    print("WhisperX HTTP服务（常驻进程模式）", flush=True)
    print("=" * 80, flush=True)
    print(f"设备：{device}", flush=True)
    print(f"计算类型：{compute_type}", flush=True)
    print("=" * 80, flush=True)
    
    # 初始化模型（只加载1次）
    init_models()
    
    print("=" * 80, flush=True)
    print("服务启动中...", flush=True)
    print("监听地址：http://0.0.0.0:5000", flush=True)
    print("健康检查：http://localhost:5000/health", flush=True)
    print("对齐接口：POST http://localhost:5000/align", flush=True)
    print("批量接口：POST http://localhost:5000/align_batch", flush=True)
    print("=" * 80, flush=True)
    
    # 启动Flask服务
    app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)
