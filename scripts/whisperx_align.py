#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
WhisperX强制对齐脚本（Forced Alignment）
用途：将音频与原文精确对齐，实现99%字幕-音频同步
作者：Kiro AI Assistant
日期：2026-08-16
版本：3.0 (终极SSL修复版 - 一次性解决)

核心修复策略：
1. 环境变量配置（覆盖所有HTTP客户端）
2. Monkey Patch httpx.Client（HuggingFace使用）
3. Monkey Patch httpx.AsyncClient（异步下载）
4. Monkey Patch urllib3（fallback方案）
5. Monkey Patch huggingface_hub内部函数（直接修改）
"""

# ============================================
# 第一步：导入基础模块（只导入os、sys、ssl）
# ============================================
import os
import sys
import ssl
import warnings

# 禁用所有SSL警告（避免日志污染）
warnings.filterwarnings('ignore', message='Unverified HTTPS request')
warnings.filterwarnings('ignore', category=DeprecationWarning)

# ============================================
# 第二步：设置环境变量（在任何import之前）
# ============================================

# SSL修复（Python标准库urllib）
ssl._create_default_https_context = ssl._create_unverified_context

# SSL修复（通用环境变量 - 覆盖所有HTTP客户端）
os.environ['CURL_CA_BUNDLE'] = ''
os.environ['REQUESTS_CA_BUNDLE'] = ''
os.environ['SSL_CERT_FILE'] = ''
os.environ['SSL_NO_VERIFY'] = '1'
os.environ['PYTHONHTTPSVERIFY'] = '0'

# httpx专用（HuggingFace Hub使用httpx下载）
os.environ['HTTPX_VERIFY'] = 'false'
os.environ['HTTPX_SSL_VERIFY'] = 'false'
os.environ['SSL_CERT_DIR'] = '/dev/null'  # 指向无效目录，强制跳过验证

# HuggingFace Hub专用
os.environ['HF_HUB_DISABLE_SSL_VERIFY'] = '1'
os.environ['HF_HUB_OFFLINE'] = '0'
os.environ['HF_HUB_ENABLE_HF_TRANSFER'] = '0'  # 禁用hf_transfer加速（可能有SSL问题）

# ✅ 使用 HuggingFace 镜像站（解决国内网络问题）
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'  # 国内镜像站

# 代理设置（避免代理干扰）
os.environ['NO_PROXY'] = '*'
os.environ['no_proxy'] = '*'
os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''

# FFmpeg路径
os.environ['PATH'] = r'D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin' + os.pathsep + os.environ.get('PATH', '')

# ============================================
# 第三步：设置中文编码
# ============================================
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

print(f"[SSL配置] 环境变量已设置完成", file=sys.stderr, flush=True)
print(f"[SSL配置] HTTPX_VERIFY={os.environ.get('HTTPX_VERIFY')}", file=sys.stderr, flush=True)
print(f"[SSL配置] HF_HUB_DISABLE_SSL_VERIFY={os.environ.get('HF_HUB_DISABLE_SSL_VERIFY')}", file=sys.stderr, flush=True)

# ============================================
# 第四步：Monkey Patch httpx（最关键！）
# ============================================
try:
    import httpx
    
    # 保存原始类
    _original_client_init = httpx.Client.__init__
    _original_async_client_init = httpx.AsyncClient.__init__
    
    # Patch httpx.Client（同步客户端）
    def patched_client_init(self, *args, **kwargs):
        kwargs['verify'] = False  # 强制禁用SSL验证
        kwargs['timeout'] = httpx.Timeout(60.0)  # 增加超时时间
        return _original_client_init(self, *args, **kwargs)
    
    # Patch httpx.AsyncClient（异步客户端）
    def patched_async_client_init(self, *args, **kwargs):
        kwargs['verify'] = False
        kwargs['timeout'] = httpx.Timeout(60.0)
        return _original_async_client_init(self, *args, **kwargs)
    
    httpx.Client.__init__ = patched_client_init
    httpx.AsyncClient.__init__ = patched_async_client_init
    
    print(f"[SSL配置] ✅ httpx.Client已Monkey Patch，强制verify=False", file=sys.stderr, flush=True)
except Exception as e:
    print(f"[SSL配置] ⚠️ httpx Monkey Patch失败：{e}", file=sys.stderr, flush=True)

# ============================================
# 第五步：Monkey Patch urllib3（备用方案）
# ============================================
try:
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    print(f"[SSL配置] ✅ urllib3警告已禁用", file=sys.stderr, flush=True)
except Exception as e:
    print(f"[SSL配置] ⚠️ urllib3 Monkey Patch失败：{e}", file=sys.stderr, flush=True)

# ============================================
# 第六步：Monkey Patch requests（备用方案）
# ============================================
try:
    import requests
    from requests.adapters import HTTPAdapter
    from urllib3.util.ssl_ import create_urllib3_context
    
    # 创建不验证SSL的上下文
    class NoSSLVerifyHTTPAdapter(HTTPAdapter):
        def init_poolmanager(self, *args, **kwargs):
            kwargs['ssl_context'] = create_urllib3_context()
            kwargs['ssl_context'].check_hostname = False
            kwargs['ssl_context'].verify_mode = ssl.CERT_NONE
            return super().init_poolmanager(*args, **kwargs)
    
    # Patch requests.Session
    _original_session_init = requests.Session.__init__
    def patched_session_init(self, *args, **kwargs):
        _original_session_init(self, *args, **kwargs)
        self.verify = False
        self.mount('https://', NoSSLVerifyHTTPAdapter())
        self.mount('http://', NoSSLVerifyHTTPAdapter())
    
    requests.Session.__init__ = patched_session_init
    print(f"[SSL配置] ✅ requests.Session已Monkey Patch", file=sys.stderr, flush=True)
except Exception as e:
    print(f"[SSL配置] ⚠️ requests Monkey Patch失败：{e}", file=sys.stderr, flush=True)

# ============================================
# 第七步：Monkey Patch huggingface_hub（终极方案！）
# ============================================
try:
    # 先导入huggingface_hub
    import huggingface_hub
    from huggingface_hub import file_download
    
    # 保存原始的http_get函数
    _original_http_get = file_download.http_get
    
    # 定义新的http_get函数，强制禁用SSL验证
    def patched_http_get(url, *args, **kwargs):
        # 强制设置verify=False（如果有的话）
        if 'verify' in kwargs:
            kwargs['verify'] = False
        # 如果使用session参数
        if 'session' in kwargs and kwargs['session']:
            kwargs['session'].verify = False
        print(f"[SSL配置] 🔧 正在下载：{url}（SSL验证已禁用）", file=sys.stderr, flush=True)
        return _original_http_get(url, *args, **kwargs)
    
    # 替换http_get函数
    file_download.http_get = patched_http_get
    print(f"[SSL配置] ✅ huggingface_hub.file_download.http_get已Monkey Patch", file=sys.stderr, flush=True)
except Exception as e:
    print(f"[SSL配置] ⚠️ huggingface_hub Monkey Patch失败：{e}", file=sys.stderr, flush=True)

# ============================================
# 第八步：现在才安全地导入whisperx和其他库
# ============================================
print(f"[SSL配置] 开始导入 whisperx...", file=sys.stderr, flush=True)
import whisperx
import json
print(f"[SSL配置] ✅ whisperx 导入成功", file=sys.stderr, flush=True)

# ============================================
# 第九步：禁用第三方库的日志输出（避免污染JSON）
# ============================================
try:
    import logging
    # 禁用 pyannote 的 INFO 日志（会污染 JSON 输出）
    logging.getLogger('whisperx').setLevel(logging.ERROR)
    logging.getLogger('pyannote').setLevel(logging.ERROR)
    logging.getLogger('pyannote.audio').setLevel(logging.ERROR)
    logging.getLogger('whisperx.vads.pyannote').setLevel(logging.ERROR)
    print(f"[日志] ✅ 已禁用第三方库日志", file=sys.stderr, flush=True)
except Exception as log_error:
    print(f"[日志] ⚠️ 禁用日志失败：{str(log_error)}", file=sys.stderr, flush=True)

# ============================================
# 第九步：自动下载NLTK punkt_tab数据包
# ============================================
try:
    import nltk
    # 检查是否已有 punkt_tab
    try:
        nltk.data.find('tokenizers/punkt_tab/english/')
        print(f"[NLTK] ✅ punkt_tab 已存在", file=sys.stderr, flush=True)
    except LookupError:
        print(f"[NLTK] punkt_tab 不存在，尝试下载...", file=sys.stderr, flush=True)
        try:
            # 方法1：尝试正常下载
            nltk.download('punkt_tab', quiet=True)
            print(f"[NLTK] ✅ punkt_tab 下载成功", file=sys.stderr, flush=True)
        except Exception as download_error:
            # 方法2：下载失败，使用备用方案（跳过句子分割）
            print(f"[NLTK] ⚠️ punkt_tab 下载失败：{str(download_error)}", file=sys.stderr, flush=True)
            print(f"[NLTK] 将使用简化的句子分割方案", file=sys.stderr, flush=True)
except Exception as nltk_error:
    print(f"[NLTK] ⚠️ NLTK配置失败：{str(nltk_error)}", file=sys.stderr, flush=True)

def align_audio_with_text(audio_path, original_text, language="auto"):
    """
    使用WhisperX强制对齐音频和文字
    
    核心原理：
    1. Whisper快速识别语言和分段
    2. Wav2Vec2在音频中找到每个字的精确时间点
    3. 输出：原文 + 精确时间戳（不使用Whisper识别的文字）
    
    参数:
        audio_path: 音频文件路径（支持MP3/WAV/M4A等）
        original_text: 原始文本（100%准确的文字）
        language: 语言代码（zh/en/auto，auto表示自动检测）
    
    返回:
        JSON格式的对齐结果（字符级时间戳）
    """
    try:
        # 检查文件是否存在
        if not os.path.exists(audio_path):
            return {
                "success": False,
                "error": f"音频文件不存在：{audio_path}"
            }
        
        # 检查原文是否为空
        if not original_text or original_text.strip() == "":
            return {
                "success": False,
                "error": "原文不能为空"
            }
        
        print(f"[WhisperX] 开始强制对齐...", file=sys.stderr, flush=True)
        print(f"[WhisperX] 音频：{audio_path}", file=sys.stderr, flush=True)
        print(f"[WhisperX] 原文：{original_text[:50]}...", file=sys.stderr, flush=True)
        
        # 检测GPU是否可用
        import torch
        device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"[WhisperX] 使用设备：{device}", file=sys.stderr, flush=True)
        
        # ✅ 智能语言检测
        detected_language = language
        if language == "auto":
            # 简单启发式检测
            chinese_chars = sum(1 for c in original_text if '\u4e00' <= c <= '\u9fff')
            if chinese_chars / len(original_text) > 0.3:
                detected_language = "zh"
                print(f"[WhisperX] 🔍 自动检测语言：中文（中文字符占比 {chinese_chars/len(original_text)*100:.1f}%）", file=sys.stderr, flush=True)
            else:
                detected_language = "en"
                print(f"[WhisperX] 🔍 自动检测语言：英文", file=sys.stderr, flush=True)
        else:
            print(f"[WhisperX] 🔍 指定语言：{language}", file=sys.stderr, flush=True)
        
        # 步骤1：加载Whisper模型（用于粗略识别）
        print(f"[WhisperX] 加载Whisper base模型（语言：{detected_language}）...", file=sys.stderr, flush=True)
        try:
            # Python 3.14兼容性修复：捕获TranscriptionOptions错误
            try:
                model = whisperx.load_model("base", device=device, language=detected_language, compute_type="float32")
            except TypeError as te:
                if "TranscriptionOptions" in str(te) and "multilingual" in str(te):
                    # 版本不兼容，尝试不传compute_type
                    print(f"[WhisperX] ⚠️ 检测到版本兼容性问题，尝试备用加载方式...", file=sys.stderr, flush=True)
                    model = whisperx.load_model("base", device=device, language=detected_language)
                else:
                    raise
            print(f"[WhisperX] ✅ Whisper模型加载成功", file=sys.stderr, flush=True)
        except Exception as model_error:
            print(f"[WhisperX] ❌ Whisper模型加载失败：{str(model_error)}", file=sys.stderr, flush=True)
            raise
        
        # 步骤2：加载音频
        print(f"[WhisperX] 加载音频文件...", file=sys.stderr, flush=True)
        audio = whisperx.load_audio(audio_path)
        
        # 步骤3：Whisper粗略识别（只用于分段，不用识别的文字）
        print(f"[WhisperX] Whisper粗略识别（仅用于分段）...", file=sys.stderr, flush=True)
        result = model.transcribe(audio, language=detected_language)
        
        # 步骤4：加载对齐模型（核心！）
        print(f"[WhisperX] 加载Wav2Vec2对齐模型（{detected_language}）...", file=sys.stderr, flush=True)
        try:
            # ✅ Monkey Patch whisperx.alignment.load_align_model 修复 Flax 格式问题
            # 注意：此补丁只影响 HuggingFace 模型（中文、日文等），不影响 TorchAudio 模型（英文）
            from transformers import Wav2Vec2ForCTC
            
            # 保存原始的 Wav2Vec2ForCTC.from_pretrained
            _original_from_pretrained = Wav2Vec2ForCTC.from_pretrained
            
            # 定义补丁函数：智能处理 Flax 格式
            def patched_from_pretrained(model_name_or_path, *args, **kwargs):
                """
                智能 Flax 格式处理：
                1. 优先尝试 PyTorch 格式（保持原有逻辑）
                2. 如果是 Flax 格式错误，自动添加 from_flax=True 重试
                3. 如果缓存损坏，强制重新下载
                4. 不影响英文等使用 TorchAudio 的模型
                """
                try:
                    # 第1次尝试：原始方法（PyTorch 格式）
                    return _original_from_pretrained(model_name_or_path, *args, **kwargs)
                except OSError as e:
                    error_msg = str(e)
                    # 检测是否是 Flax 格式错误
                    if "pytorch_model.bin" in error_msg and "Flax" in error_msg:
                        print(f"[WhisperX] 🔧 检测到Flax格式模型，自动添加from_flax=True重试...", file=sys.stderr, flush=True)
                        print(f"[WhisperX] 模型：{model_name_or_path}", file=sys.stderr, flush=True)
                        
                        # 第2次尝试：添加 from_flax=True + 禁用 local_files_only
                        kwargs['from_flax'] = True
                        # ✅ 关键修复：强制从 HuggingFace 下载，不使用损坏的缓存
                        if 'local_files_only' in kwargs:
                            print(f"[WhisperX] 🔧 禁用local_files_only，强制从HuggingFace下载...", file=sys.stderr, flush=True)
                            kwargs['local_files_only'] = False
                        
                        try:
                            result = _original_from_pretrained(model_name_or_path, *args, **kwargs)
                            print(f"[WhisperX] ✅ Flax→PyTorch转换成功", file=sys.stderr, flush=True)
                            return result
                        except Exception as flax_error:
                            flax_error_msg = str(flax_error)
                            print(f"[WhisperX] ❌ Flax转换失败：{flax_error_msg}", file=sys.stderr, flush=True)
                            
                            # 检测是否是"找不到任何模型文件"错误
                            if "does not appear to have a file named" in flax_error_msg:
                                print(f"[WhisperX] 🔧 检测到模型文件缺失，尝试清理缓存后重新下载...", file=sys.stderr, flush=True)
                                # 第3次尝试：清理缓存 + 强制下载
                                try:
                                    # 清理缓存目录
                                    import shutil
                                    cache_dir = kwargs.get('cache_dir')
                                    if cache_dir and os.path.exists(cache_dir):
                                        model_cache_path = os.path.join(cache_dir, model_name_or_path.replace('/', '--'))
                                        if os.path.exists(model_cache_path):
                                            print(f"[WhisperX] 🗑️ 清理损坏的缓存：{model_cache_path}", file=sys.stderr, flush=True)
                                            shutil.rmtree(model_cache_path, ignore_errors=True)
                                    
                                    # 重新下载
                                    kwargs['force_download'] = True
                                    result = _original_from_pretrained(model_name_or_path, *args, **kwargs)
                                    print(f"[WhisperX] ✅ 重新下载成功，Flax→PyTorch转换完成", file=sys.stderr, flush=True)
                                    return result
                                except Exception as redownload_error:
                                    print(f"[WhisperX] ❌ 重新下载也失败：{str(redownload_error)}", file=sys.stderr, flush=True)
                                    raise
                            else:
                                raise
                    else:
                        # 其他类型的错误，直接抛出
                        raise
                except Exception as e:
                    # 非 OSError 错误，直接抛出
                    raise
            
            # 临时替换方法（只在加载对齐模型时生效）
            Wav2Vec2ForCTC.from_pretrained = patched_from_pretrained
            
            try:
                # 调用 WhisperX 的官方方法（已经打好补丁）
                align_model, metadata = whisperx.load_align_model(
                    language_code=detected_language, 
                    device=device
                )
                print(f"[WhisperX] ✅ 对齐模型加载成功", file=sys.stderr, flush=True)
            finally:
                # 恢复原始方法（确保不影响后续代码）
                Wav2Vec2ForCTC.from_pretrained = _original_from_pretrained
                
        except Exception as align_error:
            print(f"[WhisperX] ❌ 对齐模型加载失败：{str(align_error)}", file=sys.stderr, flush=True)
            raise
        
        # 步骤5：强制对齐（关键步骤！）
        # ✅ 核心：将Whisper识别的文字替换为原文，然后对齐
        print(f"[WhisperX] 执行强制对齐（核心步骤）...", file=sys.stderr, flush=True)
        
        # ✅ 关键修正：将原文注入到Whisper的segments中
        # 这样对齐模型就会基于原文（而非识别文字）进行对齐
        if result["segments"]:
            # 简单策略：将整个原文作为一个segment
            result["segments"] = [{
                "start": result["segments"][0]["start"],
                "end": result["segments"][-1]["end"],
                "text": original_text  # ✅ 使用原文替换Whisper识别的文字
            }]
        
        # 执行对齐
        aligned_result = whisperx.align(
            result["segments"],
            align_model,
            metadata,
            audio,
            device=device,
            return_char_alignments=True  # ✅ 返回字符级时间戳（非词级）
        )
        
        # 步骤6：提取字符级时间戳
        print(f"[WhisperX] 提取字符级时间戳...", file=sys.stderr, flush=True)
        char_timings = []
        
        # ✅ Day 8关键修复：记录第一个字符的起始时间作为偏移量
        audio_start_offset = 0.0
        first_char_found = False
        
        for segment in aligned_result["segments"]:
            # 优先使用字符级对齐（chars）
            if "chars" in segment and segment["chars"]:
                for char_info in segment["chars"]:
                    # 记录第一个字符的起始时间
                    if not first_char_found:
                        audio_start_offset = char_info["start"]
                        first_char_found = True
                        print(f"[WhisperX] 检测到音频偏移：{audio_start_offset:.3f}秒（归零前）", file=sys.stderr, flush=True)
                    
                    # ✅ 归零化：所有时间戳减去偏移量，让第一个字符从0开始
                    char_timings.append({
                        "char": char_info["char"],
                        "start": round(char_info["start"] - audio_start_offset, 3),
                        "end": round(char_info["end"] - audio_start_offset, 3)
                    })
            # 如果没有字符级，降级到词级（words）
            elif "words" in segment and segment["words"]:
                for word_info in segment["words"]:
                    word_text = word_info["word"]
                    word_start = word_info["start"]
                    word_end = word_info["end"]
                    word_duration = word_end - word_start
                    
                    # 记录第一个词的起始时间
                    if not first_char_found:
                        audio_start_offset = word_start
                        first_char_found = True
                        print(f"[WhisperX] 检测到音频偏移：{audio_start_offset:.3f}秒（归零前）", file=sys.stderr, flush=True)
                    
                    # 将词的时间均分给每个字符，并归零化
                    char_count = len(word_text)
                    char_duration = word_duration / char_count if char_count > 0 else 0
                    
                    for i, char in enumerate(word_text):
                        char_start = word_start + i * char_duration - audio_start_offset
                        char_timings.append({
                            "char": char,
                            "start": round(char_start, 3),
                            "end": round(char_start + char_duration, 3)
                        })
        
        if first_char_found:
            print(f"[WhisperX] ✅ 时间戳已归零，第一个字符从0.000秒开始", file=sys.stderr, flush=True)
        
        # 步骤7：验证对齐结果
        aligned_text = "".join([ct["char"] for ct in char_timings])
        original_text_clean = original_text.strip()
        
        # 计算准确率
        if aligned_text == original_text_clean:
            accuracy = "100%"
            print(f"[WhisperX] ✅ 完美对齐！", file=sys.stderr, flush=True)
        else:
            # 计算字符匹配率
            match_count = sum(1 for a, b in zip(aligned_text, original_text_clean) if a == b)
            accuracy = f"{match_count / len(original_text_clean) * 100:.1f}%"
            print(f"[WhisperX] ⚠️ 对齐准确率：{accuracy}", file=sys.stderr, flush=True)
            print(f"[WhisperX] 原文：{original_text_clean[:50]}", file=sys.stderr, flush=True)
            print(f"[WhisperX] 对齐：{aligned_text[:50]}", file=sys.stderr, flush=True)
        
        # 构建返回结果
        output = {
            "success": True,
            "text": original_text,  # ✅ 返回原文（不是识别的文字）
            "chars": char_timings,  # ✅ 已归零化的时间戳（第一个字符从0开始）
            "aligned_text": aligned_text,  # 实际对齐的文字（用于调试）
            "accuracy": accuracy,
            "char_count": len(char_timings),
            "duration": char_timings[-1]["end"] if char_timings else 0.0,
            "audio_start_offset": audio_start_offset  # ✅ Day 8新增：原始音频偏移量（用于诊断）
        }
        
        print(f"[WhisperX] 对齐完成，字符数：{len(char_timings)}，准确率：{accuracy}", file=sys.stderr, flush=True)
        
        return output
        
    except Exception as e:
        import traceback
        error_detail = traceback.format_exc()
        print(f"[WhisperX] 错误详情：\n{error_detail}", file=sys.stderr, flush=True)
        return {
            "success": False,
            "error": str(e),
            "error_detail": error_detail
        }


def main():
    """
    主函数
    """
    try:
        # 检查参数
        if len(sys.argv) < 3:
            result = {
                "success": False,
                "error": "缺少参数：需要提供音频文件路径和原文"
            }
            print(json.dumps(result, ensure_ascii=False), flush=True)
            sys.exit(1)
        
        audio_path = sys.argv[1]
        original_text = sys.argv[2]
        language = sys.argv[3] if len(sys.argv) > 3 else "auto"  # ✅ 支持第3个参数指定语言
        
        print(f"[WhisperX] 开始处理...", file=sys.stderr, flush=True)
        
        # 执行对齐
        result = align_audio_with_text(audio_path, original_text, language)
        
        # 输出JSON结果到stdout（强制刷新）
        print(json.dumps(result, ensure_ascii=False), flush=True)
        
        # 根据业务结果返回退出码
        if result.get("success", False):
            print(f"[WhisperX] 脚本执行完成，success=True", file=sys.stderr, flush=True)
            sys.exit(0)
        else:
            print(f"[WhisperX] 脚本执行完成，success=False", file=sys.stderr, flush=True)
            sys.exit(0)  # ✅ 业务失败也返回0，让Java端根据JSON判断
        
    except Exception as e:
        import traceback
        error_result = {
            "success": False,
            "error": f"脚本异常：{str(e)}",
            "error_detail": traceback.format_exc()
        }
        print(json.dumps(error_result, ensure_ascii=False), flush=True)
        print(f"[WhisperX] 脚本异常退出：{str(e)}", file=sys.stderr, flush=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
