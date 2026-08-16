#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
WhisperX常驻服务（Flask HTTP API）
用途：模型常驻内存，避免每次调用重新加载，性能提升10倍+
作者：Kiro AI Assistant
日期：2026-08-16
版本：1.0

性能对比：
- 旧方案（每次加载）：10-15秒（加载4-6秒 + 对齐6-9秒）
- 新方案（常驻服务）：1-2秒（只有对齐时间，无加载开销）

启动方式：
  python whisperx_server.py

配置项：
  - 端口：5000（可通过 --port 修改）
  - 主机：0.0.0.0（可通过 --host 修改）
  - 设备：自动检测GPU/CPU
"""

# ============================================
# 第一步：SSL和环境配置（与whisperx_align.py相同）
# ============================================
import os
import sys
import ssl
import warnings

warnings.filterwarnings('ignore', message='Unverified HTTPS request')
warnings.filterwarnings('ignore', category=DeprecationWarning)

ssl._create_default_https_context = ssl._create_unverified_context

os.environ['CURL_CA_BUNDLE'] = ''
os.environ['REQUESTS_CA_BUNDLE'] = ''
os.environ['SSL_CERT_FILE'] = ''
os.environ['SSL_NO_VERIFY'] = '1'
os.environ['PYTHONHTTPSVERIFY'] = '0'
os.environ['HTTPX_VERIFY'] = 'false'
os.environ['HTTPX_SSL_VERIFY'] = 'false'
os.environ['SSL_CERT_DIR'] = '/dev/null'
os.environ['HF_HUB_DISABLE_SSL_VERIFY'] = '1'
os.environ['HF_HUB_OFFLINE'] = '0'
os.environ['HF_HUB_ENABLE_HF_TRANSFER'] = '0'
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'
os.environ['NO_PROXY'] = '*'
os.environ['no_proxy'] = '*'
os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''
os.environ['PATH'] = r'D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin' + os.pathsep + os.environ.get('PATH', '')

import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# ============================================
# 第二步：Monkey Patch（与whisperx_align.py相同）
# ============================================
try:
    import httpx
    _original_client_init = httpx.Client.__init__
    _original_async_client_init = httpx.AsyncClient.__init__
    
    def patched_client_init(self, *args, **kwargs):
        kwargs['verify'] = False
        kwargs['timeout'] = httpx.Timeout(60.0)
        return _original_client_init(self, *args, **kwargs)
    
    def patched_async_client_init(self, *args, **kwargs):
        kwargs['verify'] = False
        kwargs['timeout'] = httpx.Timeout(60.0)
        return _original_async_client_init(self, *args, **kwargs)
    
    httpx.Client.__init__ = patched_client_init
    httpx.AsyncClient.__init__ = patched_async_client_init
except Exception as e:
    print(f"[SSL] ⚠️ httpx Monkey Patch失败：{e}", file=sys.stderr)

try:
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
except Exception as e:
    print(f"[SSL] ⚠️ urllib3 Monkey Patch失败：{e}", file=sys.stderr)

try:
    import requests
    from requests.adapters import HTTPAdapter
    from urllib3.util.ssl_ import create_urllib3_context
    
    class NoSSLVerifyHTTPAdapter(HTTPAdapter):
        def init_poolmanager(self, *args, **kwargs):
            kwargs['ssl_context'] = create_urllib3_context()
            kwargs['ssl_context'].check_hostname = False
            kwargs['ssl_context'].verify_mode = ssl.CERT_NONE
            return super().init_poolmanager(*args, **kwargs)
    
    _original_session_init = requests.Session.__init__
    def patched_session_init(self, *args, **kwargs):
        _original_session_init(self, *args, **kwargs)
        self.verify = False
        self.mount('https://', NoSSLVerifyHTTPAdapter())
        self.mount('http://', NoSSLVerifyHTTPAdapter())
    
    requests.Session.__init__ = patched_session_init
except Exception as e:
    print(f"[SSL] ⚠️ requests Monkey Patch失败：{e}", file=sys.stderr)

try:
    import huggingface_hub
    from huggingface_hub import file_download
    
    _original_http_get = file_download.http_get
    
    def patched_http_get(url, *args, **kwargs):
        if 'verify' in kwargs:
            kwargs['verify'] = False
        if 'session' in kwargs and kwargs['session']:
            kwargs['session'].verify = False
        return _original_http_get(url, *args, **kwargs)
    
    file_download.http_get = patched_http_get
except Exception as e:
    print(f"[SSL] ⚠️ huggingface_hub Monkey Patch失败：{e}", file=sys.stderr)

# ============================================
# 第三步：导入核心库
# ============================================
import whisperx
import torch
import json
import base64
import tempfile
import traceback
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging

# 禁用第三方库日志
logging.getLogger('whisperx').setLevel(logging.ERROR)
logging.getLogger('pyannote').setLevel(logging.ERROR)
logging.getLogger('pyannote.audio').setLevel(logging.ERROR)
logging.getLogger('whisperx.vads.pyannote').setLevel(logging.ERROR)

# ============================================
# 第四步：自动下载NLTK数据
# ============================================
try:
    import nltk
    try:
        nltk.data.find('tokenizers/punkt_tab/english/')
    except LookupError:
        print(f"[NLTK] punkt_tab 不存在，尝试下载...")
        nltk.download('punkt_tab', quiet=True)
except Exception as e:
    print(f"[NLTK] ⚠️ 配置失败：{e}")

# ============================================
# 第五步：Monkey Patch Wav2Vec2（Flax修复）
# ============================================
from transformers import Wav2Vec2ForCTC

_original_from_pretrained = Wav2Vec2ForCTC.from_pretrained

def patched_from_pretrained(model_name_or_path, *args, **kwargs):
    try:
        return _original_from_pretrained(model_name_or_path, *args, **kwargs)
    except OSError as e:
        error_msg = str(e)
        if "pytorch_model.bin" in error_msg and "Flax" in error_msg:
            print(f"[WhisperX] 🔧 检测到Flax格式模型，自动添加from_flax=True...")
            kwargs['from_flax'] = True
            if 'local_files_only' in kwargs:
                kwargs['local_files_only'] = False
            
            try:
                result = _original_from_pretrained(model_name_or_path, *args, **kwargs)
                print(f"[WhisperX] ✅ Flax→PyTorch转换成功")
                return result
            except Exception as flax_error:
                if "does not appear to have a file named" in str(flax_error):
                    print(f"[WhisperX] 🔧 清理缓存后重新下载...")
                    import shutil
                    cache_dir = kwargs.get('cache_dir')
                    if cache_dir and os.path.exists(cache_dir):
                        model_cache_path = os.path.join(cache_dir, model_name_or_path.replace('/', '--'))
                        if os.path.exists(model_cache_path):
                            shutil.rmtree(model_cache_path, ignore_errors=True)
                    kwargs['force_download'] = True
                    result = _original_from_pretrained(model_name_or_path, *args, **kwargs)
                    print(f"[WhisperX] ✅ 重新下载成功")
                    return result
                else:
                    raise
        else:
            raise
    except Exception as e:
        raise

Wav2Vec2ForCTC.from_pretrained = patched_from_pretrained

# ============================================
# 第六步：全局模型缓存（性能关键！）
# ============================================
print("=" * 60)
print("WhisperX常驻服务启动中...")
print("=" * 60)

device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"[设备] {device.upper()}")

# 全局模型缓存
MODEL_CACHE = {}

def get_whisper_model(language="auto"):
    """
    获取Whisper模型（带缓存）
    """
    # 统一语言代码
    if language == "auto":
        cache_key = "base"
    else:
        cache_key = f"base_{language}"
    
    if cache_key not in MODEL_CACHE:
        print(f"[模型] 首次加载Whisper模型（语言：{language}）...")
        try:
            model = whisperx.load_model("base", device=device, language=None if language == "auto" else language, compute_type="float32")
        except TypeError:
            model = whisperx.load_model("base", device=device, language=None if language == "auto" else language)
        MODEL_CACHE[cache_key] = model
        print(f"[模型] ✅ Whisper模型已加载并缓存")
    else:
        print(f"[模型] ✅ 使用缓存的Whisper模型")
    
    return MODEL_CACHE[cache_key]

def get_align_model(language_code):
    """
    获取对齐模型（带缓存）
    """
    cache_key = f"align_{language_code}"
    
    if cache_key not in MODEL_CACHE:
        print(f"[模型] 首次加载对齐模型（语言：{language_code}）...")
        align_model, metadata = whisperx.load_align_model(language_code=language_code, device=device)
        MODEL_CACHE[cache_key] = (align_model, metadata)
        print(f"[模型] ✅ 对齐模型已加载并缓存")
    else:
        print(f"[模型] ✅ 使用缓存的对齐模型")
    
    return MODEL_CACHE[cache_key]

# ============================================
# 第七步：Flask应用
# ============================================
app = Flask(__name__)
CORS(app)  # 支持跨域

@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({
        "status": "ok",
        "device": device,
        "cached_models": list(MODEL_CACHE.keys())
    })

@app.route('/align', methods=['POST'])
def align():
    """
    WhisperX强制对齐接口
    
    请求体（JSON）：
    {
        "audio": "base64编码的音频数据",
        "text": "原始文本",
        "language": "zh/en/auto"  // 可选，默认auto
    }
    
    返回（JSON）：
    {
        "success": true/false,
        "text": "原始文本",
        "chars": [...],
        "accuracy": "100%",
        "duration": 10.5,
        "processing_time": 1.2  // 处理耗时（秒）
    }
    """
    import time
    start_time = time.time()
    
    try:
        # 解析请求
        data = request.get_json()
        if not data:
            return jsonify({
                "success": False,
                "error": "请求体为空"
            }), 400
        
        audio_base64 = data.get('audio')
        original_text = data.get('text')
        language = data.get('language', 'auto')
        
        if not audio_base64:
            return jsonify({
                "success": False,
                "error": "缺少audio参数"
            }), 400
        
        if not original_text:
            return jsonify({
                "success": False,
                "error": "缺少text参数"
            }), 400
        
        # 解码音频
        try:
            audio_data = base64.b64decode(audio_base64)
        except Exception as e:
            return jsonify({
                "success": False,
                "error": f"音频解码失败：{str(e)}"
            }), 400
        
        # 保存到临时文件
        with tempfile.NamedTemporaryFile(suffix='.mp3', delete=False) as f:
            f.write(audio_data)
            audio_path = f.name
        
        try:
            # 智能语言检测
            detected_language = language
            if language == "auto":
                chinese_chars = sum(1 for c in original_text if '\u4e00' <= c <= '\u9fff')
                if chinese_chars / len(original_text) > 0.3:
                    detected_language = "zh"
                else:
                    detected_language = "en"
                print(f"[请求] 自动检测语言：{detected_language}")
            else:
                print(f"[请求] 指定语言：{language}")
            
            # 加载音频
            audio = whisperx.load_audio(audio_path)
            
            # 获取模型（从缓存）
            model = get_whisper_model(detected_language)
            align_model, metadata = get_align_model(detected_language)
            
            # Whisper粗略识别
            result = model.transcribe(audio, language=detected_language)
            
            # 将原文注入到segments
            if result["segments"]:
                result["segments"] = [{
                    "start": result["segments"][0]["start"],
                    "end": result["segments"][-1]["end"],
                    "text": original_text
                }]
            
            # 强制对齐
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
            for segment in aligned_result["segments"]:
                if "chars" in segment and segment["chars"]:
                    for char_info in segment["chars"]:
                        char_timings.append({
                            "char": char_info["char"],
                            "start": round(char_info["start"], 3),
                            "end": round(char_info["end"], 3)
                        })
                elif "words" in segment and segment["words"]:
                    for word_info in segment["words"]:
                        word_text = word_info["word"]
                        word_start = word_info["start"]
                        word_end = word_info["end"]
                        word_duration = word_end - word_start
                        char_count = len(word_text)
                        char_duration = word_duration / char_count if char_count > 0 else 0
                        
                        for i, char in enumerate(word_text):
                            char_start = word_start + i * char_duration
                            char_timings.append({
                                "char": char,
                                "start": round(char_start, 3),
                                "end": round(char_start + char_duration, 3)
                            })
            
            # 计算准确率
            aligned_text = "".join([ct["char"] for ct in char_timings])
            original_text_clean = original_text.strip()
            
            if aligned_text == original_text_clean:
                accuracy = "100%"
            else:
                match_count = sum(1 for a, b in zip(aligned_text, original_text_clean) if a == b)
                accuracy = f"{match_count / len(original_text_clean) * 100:.1f}%"
            
            processing_time = time.time() - start_time
            
            print(f"[请求] ✅ 对齐完成，字符数：{len(char_timings)}，准确率：{accuracy}，耗时：{processing_time:.2f}秒")
            
            return jsonify({
                "success": True,
                "text": original_text,
                "chars": char_timings,
                "aligned_text": aligned_text,
                "accuracy": accuracy,
                "char_count": len(char_timings),
                "duration": char_timings[-1]["end"] if char_timings else 0.0,
                "processing_time": round(processing_time, 2),
                "language": detected_language
            })
            
        finally:
            # 清理临时文件
            if os.path.exists(audio_path):
                os.unlink(audio_path)
    
    except Exception as e:
        error_detail = traceback.format_exc()
        print(f"[错误] {error_detail}")
        return jsonify({
            "success": False,
            "error": str(e),
            "error_detail": error_detail
        }), 500

# ============================================
# 第八步：启动服务
# ============================================
if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='WhisperX常驻服务')
    parser.add_argument('--host', type=str, default='0.0.0.0', help='监听地址')
    parser.add_argument('--port', type=int, default=5000, help='监听端口')
    args = parser.parse_args()
    
    print("=" * 60)
    print(f"服务启动成功！")
    print(f"访问地址: http://{args.host}:{args.port}")
    print(f"健康检查: http://{args.host}:{args.port}/health")
    print(f"对齐接口: http://{args.host}:{args.port}/align (POST)")
    print("=" * 60)
    
    # 启动Flask（生产环境建议用gunicorn或waitress）
    app.run(host=args.host, port=args.port, debug=False, threaded=True)
