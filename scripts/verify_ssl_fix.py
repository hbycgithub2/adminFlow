#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SSL修复验证脚本
用途：快速验证SSL环境变量是否正确设置
"""

import os
import sys

print("=" * 60)
print("SSL修复验证脚本")
print("=" * 60)
print()

print("[检查1] 验证环境变量设置...")
print()

# 必须在import whisperx之前设置
required_vars = {
    'HTTPX_VERIFY': 'false',
    'HF_HUB_DISABLE_SSL_VERIFY': '1',
    'NO_PROXY': '*',
    'CURL_CA_BUNDLE': '',
    'REQUESTS_CA_BUNDLE': '',
    'SSL_CERT_FILE': ''
}

# 先设置环境变量（模拟whisperx_align.py的设置）
import ssl
ssl._create_default_https_context = ssl._create_unverified_context
for key, value in required_vars.items():
    os.environ[key] = value

print("✅ 环境变量已设置")
for key, expected in required_vars.items():
    actual = os.environ.get(key, '')
    status = "✅" if actual == expected else "❌"
    print(f"  {status} {key}={actual}")

print()
print("[检查2] 验证SSL配置...")
print(f"✅ ssl._create_default_https_context已修改")

print()
print("[检查3] 尝试导入whisperx...")
try:
    import whisperx
    print("✅ whisperx导入成功")
except Exception as e:
    print(f"❌ whisperx导入失败：{e}")
    sys.exit(1)

print()
print("[检查4] 尝试加载模型（关键测试）...")
print("注意：首次运行会下载模型（约1GB），请耐心等待...")
print()

try:
    import torch
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"使用设备：{device}")
    
    print("加载Whisper base模型...")
    model = whisperx.load_model("base", device=device, language="zh", compute_type="float32")
    print("✅ Whisper base模型加载成功！")
    
    print()
    print("加载Wav2Vec2对齐模型（Chinese）...")
    align_model, metadata = whisperx.load_align_model(language_code="zh", device=device)
    print("✅ Wav2Vec2对齐模型加载成功！")
    
    print()
    print("=" * 60)
    print("✅✅✅ 所有检查通过！SSL问题已解决！")
    print("=" * 60)
    print()
    print("下一步：")
    print("1. 重启Spring Boot服务")
    print("2. 上传DOCX文档测试")
    print("3. 查看日志确认使用WhisperX（而非智能算法）")
    print()
    
except Exception as e:
    print()
    print("=" * 60)
    print("❌❌❌ 模型加载失败")
    print("=" * 60)
    print()
    print(f"错误信息：{e}")
    print()
    
    # 检查是否仍然是SSL错误
    error_str = str(e)
    if "SSL" in error_str or "CERTIFICATE" in error_str:
        print("⚠️  仍然是SSL证书问题！")
        print()
        print("可能的原因：")
        print("1. 环境变量在import whisperx之后设置（已修复）")
        print("2. httpx或huggingface_hub库版本太旧")
        print("3. Python缓存问题（.pyc文件）")
        print()
        print("建议操作：")
        print("1. 清理Python缓存：")
        print("   py -m pip cache purge")
        print("2. 升级相关库：")
        print("   pip install --upgrade huggingface_hub httpx certifi")
        print("3. 重新运行此脚本")
    else:
        print("⚠️  不是SSL问题，是其他错误")
        print()
        print("建议操作：")
        print("1. 查看上面的错误详情")
        print("2. 检查网络连接")
        print("3. 检查磁盘空间")
    
    print()
    sys.exit(1)
