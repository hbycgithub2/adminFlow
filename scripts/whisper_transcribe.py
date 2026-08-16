#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Whisper音频识别脚本
用途：从Java调用Whisper进行音频识别,返回逐字时间戳
作者：Kiro AI Assistant
日期：2026-08-14
"""

import whisper
import json
import sys
import os

# ✅ 修复FFmpeg路径问题：添加FFmpeg到PATH环境变量
os.environ['PATH'] = r'D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin' + os.pathsep + os.environ.get('PATH', '')

# ✅ 修复中文编码问题：强制UTF-8输出
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

def transcribe_audio(audio_path, prompt_text=None):
    """
    识别音频，返回逐字时间戳
    
    参数:
        audio_path: 音频文件路径
        prompt_text: 提示文本（可选），帮助Whisper更准确识别
    
    返回:
        JSON格式的识别结果
    """
    try:
        # 检查文件是否存在
        if not os.path.exists(audio_path):
            return {
                "success": False,
                "error": f"音频文件不存在：{audio_path}"
            }
        
        # 加载Whisper base模型（输出到stderr）
        print(f"[Whisper] 加载base模型...", file=sys.stderr, flush=True)
        model = whisper.load_model("base")
        
        # 识别音频（带逐字时间戳）（输出到stderr）
        print(f"[Whisper] 识别音频：{audio_path}", file=sys.stderr, flush=True)
        
        # ✅ 关键改进：添加prompt参数，提示Whisper应该识别的内容
        transcribe_options = {
            "language": "zh",  # 中文
            "word_timestamps": True  # 启用逐字时间戳
        }
        
        # 如果提供了提示文本，加入prompt
        if prompt_text:
            transcribe_options["initial_prompt"] = prompt_text
            print(f"[Whisper] 使用提示文本：{prompt_text}", file=sys.stderr, flush=True)
        
        result = model.transcribe(audio_path, **transcribe_options)
        
        # 提取逐字信息
        words = []
        for segment in result["segments"]:
            if "words" in segment:
                for word in segment["words"]:
                    words.append({
                        "text": word["word"].strip(),
                        "start": round(word["start"], 3),
                        "end": round(word["end"], 3)
                    })
        
        # 构建返回结果
        output = {
            "success": True,
            "text": result["text"],
            "words": words,
            "language": result["language"],
            "duration": result.get("duration", 0)
        }
        
        print(f"[Whisper] 识别完成，字数：{len(words)}", file=sys.stderr, flush=True)
        
        return output
        
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }


def main():
    """
    主函数
    """
    try:
        # 检查参数
        if len(sys.argv) < 2:
            result = {
                "success": False,
                "error": "缺少参数：需要提供音频文件路径"
            }
            print(json.dumps(result, ensure_ascii=False), flush=True)
            sys.exit(1)
        
        audio_path = sys.argv[1]
        
        # ✅ 新增：支持可选的第二个参数（提示文本）
        prompt_text = sys.argv[2] if len(sys.argv) >= 3 else None
        
        # 识别音频
        result = transcribe_audio(audio_path, prompt_text)
        
        # 输出JSON结果到stdout（强制刷新）
        print(json.dumps(result, ensure_ascii=False), flush=True)
        
        # ✅ 关键修复：即使识别失败（success=False），也返回0
        # 原因：识别失败不是脚本错误，而是业务逻辑，应该正常退出
        # Java端会根据JSON中的success字段判断业务成功与否
        print(f"[Whisper] 脚本执行完成，success={result.get('success', False)}", file=sys.stderr, flush=True)
        sys.exit(0)
        
    except Exception as e:
        # 只有脚本本身出错（非业务错误）才返回1
        error_result = {
            "success": False,
            "error": f"脚本异常：{str(e)}"
        }
        print(json.dumps(error_result, ensure_ascii=False), flush=True)
        print(f"[Whisper] 脚本异常退出：{str(e)}", file=sys.stderr, flush=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
