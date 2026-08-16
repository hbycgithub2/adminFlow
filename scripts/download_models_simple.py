#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
WhisperX 模型一键下载脚本（完全绕过SSL验证）
用途：手动下载模型文件，彻底解决SSL证书问题
日期：2026-08-16
"""

import os
import sys
import ssl
import warnings

# 禁用所有SSL警告
warnings.filterwarnings('ignore')

# 强制禁用SSL验证（必须在import之前）
ssl._create_default_https_context = ssl._create_unverified_context

# 设置环境变量
os.environ['HF_HUB_DISABLE_SSL_VERIFY'] = '1'
os.environ['NO_PROXY'] = '*'
os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
os.environ['CURL_CA_BUNDLE'] = ''
os.environ['REQUESTS_CA_BUNDLE'] = ''
os.environ['SSL_CERT_FILE'] = ''
os.environ['PYTHONHTTPSVERIFY'] = '0'

print("[下载器] SSL验证已完全禁用")

# ============================================
# 关键修复：Monkey Patch httpx（HuggingFace使用httpx下载）
# ============================================
print("[下载器] 开始 Monkey Patch httpx...")
try:
    import httpx
    
    # 保存原始类
    _original_client_init = httpx.Client.__init__
    _original_async_client_init = httpx.AsyncClient.__init__
    
    # Patch httpx.Client（同步客户端）
    def patched_client_init(self, *args, **kwargs):
        kwargs['verify'] = False  # 强制禁用SSL验证
        kwargs['timeout'] = httpx.Timeout(120.0)  # 增加超时时间
        return _original_client_init(self, *args, **kwargs)
    
    # Patch httpx.AsyncClient（异步客户端）
    def patched_async_client_init(self, *args, **kwargs):
        kwargs['verify'] = False
        kwargs['timeout'] = httpx.Timeout(120.0)
        return _original_async_client_init(self, *args, **kwargs)
    
    httpx.Client.__init__ = patched_client_init
    httpx.AsyncClient.__init__ = patched_async_client_init
    
    print("[下载器] ✅ httpx Monkey Patch 成功")
except Exception as e:
    print(f"[下载器] ⚠️ httpx Monkey Patch 失败：{e}")

# ============================================
# 备用修复：Monkey Patch urllib3
# ============================================
try:
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    print("[下载器] ✅ urllib3 警告已禁用")
except Exception as e:
    print(f"[下载器] ⚠️ urllib3 修复失败：{e}")

print("[下载器] 开始导入 huggingface_hub...")

try:
    from huggingface_hub import snapshot_download, hf_hub_download
    print("[下载器] ✅ huggingface_hub 导入成功")
except ImportError:
    print("[下载器] ❌ huggingface_hub 未安装")
    print("[下载器] 正在安装 huggingface_hub...")
    os.system('py -3.13 -m pip install huggingface_hub -i https://pypi.tuna.tsinghua.edu.cn/simple')
    from huggingface_hub import snapshot_download, hf_hub_download
    print("[下载器] ✅ huggingface_hub 安装完成")

# 模型配置
MODELS = {
    "whisper_base": {
        "repo_id": "Systran/faster-whisper-base",
        "name": "Whisper Base 模型",
        "size": "约150MB",
        "description": "用于语音识别和分段"
    },
    "wav2vec2_chinese": {
        "repo_id": "jonatasgrosman/wav2vec2-large-xlsr-53-chinese-zh-cn",
        "name": "Wav2Vec2 中文对齐模型",
        "size": "约1.2GB",
        "description": "用于字符级时间对齐（核心！）"
    }
}

def download_model(model_key):
    """
    下载单个模型
    """
    model_info = MODELS[model_key]
    print(f"\n{'='*50}")
    print(f"开始下载：{model_info['name']}")
    print(f"{'='*50}")
    print(f"仓库：{model_info['repo_id']}")
    print(f"大小：{model_info['size']}")
    print(f"用途：{model_info['description']}")
    print()
    
    try:
        # 使用 snapshot_download 下载整个模型
        cache_dir = snapshot_download(
            repo_id=model_info['repo_id'],
            cache_dir=None,  # 使用默认缓存目录
            resume_download=True,  # 支持断点续传
            local_files_only=False
        )
        
        print(f"\n✅ {model_info['name']} 下载成功！")
        print(f"📁 缓存位置：{cache_dir}")
        return True
        
    except Exception as e:
        print(f"\n❌ {model_info['name']} 下载失败！")
        print(f"错误信息：{str(e)}")
        return False

def main():
    """
    主函数
    """
    print("="*50)
    print("WhisperX 模型一键下载器")
    print("="*50)
    print()
    print("此脚本将下载以下模型：")
    for idx, (key, info) in enumerate(MODELS.items(), 1):
        print(f"{idx}. {info['name']} ({info['size']})")
        print(f"   {info['description']}")
    print()
    
    # 确认下载
    choice = input("是否开始下载？[Y/n]: ").strip().lower()
    if choice and choice != 'y':
        print("取消下载。")
        return
    
    # 下载所有模型
    success_count = 0
    total_count = len(MODELS)
    
    for model_key in MODELS.keys():
        if download_model(model_key):
            success_count += 1
    
    # 总结
    print("\n" + "="*50)
    print("下载完成")
    print("="*50)
    print(f"成功：{success_count}/{total_count}")
    
    if success_count == total_count:
        print("\n✅ 所有模型下载成功！")
        print("\n下一步操作：")
        print("1. 启动 Java 服务（hm-service）")
        print("2. 上传 Word 文档测试")
        print("3. 观察 WhisperX 对齐结果")
    else:
        print("\n⚠️ 部分模型下载失败")
        print("建议：")
        print("1. 检查网络连接")
        print("2. 重新运行此脚本（支持断点续传）")
        print("3. 如果持续失败，尝试使用浏览器手动下载")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n下载已取消。")
    except Exception as e:
        print(f"\n\n脚本执行失败：{str(e)}")
        import traceback
        traceback.print_exc()
