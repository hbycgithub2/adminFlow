#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查 WhisperX 模型是否已下载"""

import os
import sys

# 设置中文编码
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def check_models():
    """检查模型下载状态"""
    
    # HuggingFace缓存目录
    cache_dir = os.path.expanduser('~/.cache/huggingface/hub')
    
    print("="*60)
    print("WhisperX 模型下载状态检查")
    print("="*60)
    print()
    
    print(f"📁 缓存目录: {cache_dir}")
    print(f"📊 是否存在: {'✅ 是' if os.path.exists(cache_dir) else '❌ 否'}")
    print()
    
    if not os.path.exists(cache_dir):
        print("❌ 模型尚未下载")
        print()
        print("请运行以下命令下载模型:")
        print("  cd d:\\code\\adminFlow\\scripts")
        print("  一键下载模型.bat")
        return False
    
    # 检查具体模型
    models = {
        "Whisper Base": "models--Systran--faster-whisper-base",
        "Wav2Vec2 中文": "models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn"
    }
    
    all_found = True
    
    for model_name, model_dir in models.items():
        model_path = os.path.join(cache_dir, model_dir)
        exists = os.path.exists(model_path)
        
        print(f"[模型] {model_name}")
        print(f"  状态: {'✅ 已下载' if exists else '❌ 未下载'}")
        
        if exists:
            # 计算目录大小
            total_size = 0
            file_count = 0
            
            for root, dirs, files in os.walk(model_path):
                for file in files:
                    file_path = os.path.join(root, file)
                    if os.path.exists(file_path):
                        total_size += os.path.getsize(file_path)
                        file_count += 1
            
            size_mb = total_size / (1024 * 1024)
            print(f"  大小: {size_mb:.2f} MB")
            print(f"  文件数: {file_count}")
            
            # 检查关键文件
            key_files = {
                "Whisper Base": ["model.bin", "config.json"],
                "Wav2Vec2 中文": ["pytorch_model.bin", "config.json"]
            }
            
            if model_name in key_files:
                missing_files = []
                for key_file in key_files[model_name]:
                    found = False
                    for root, dirs, files in os.walk(model_path):
                        if key_file in files:
                            found = True
                            break
                    if not found:
                        missing_files.append(key_file)
                
                if missing_files:
                    print(f"  ⚠️ 缺失关键文件: {', '.join(missing_files)}")
                    all_found = False
                else:
                    print(f"  ✅ 关键文件完整")
        else:
            all_found = False
        
        print()
    
    # 总结
    print("="*60)
    if all_found:
        print("✅ 所有模型已完整下载，可以开始使用！")
        print()
        print("下一步操作:")
        print("  1. 启动 Java 服务: cd hm-service && mvn spring-boot:run")
        print("  2. 上传 Word 文档测试")
        print("  3. 观察日志中的 WhisperX 对齐结果")
    else:
        print("❌ 模型未完整下载")
        print()
        print("请运行以下命令下载:")
        print("  cd d:\\code\\adminFlow\\scripts")
        print("  一键下载模型.bat")
    print("="*60)
    
    return all_found

if __name__ == "__main__":
    try:
        check_models()
    except Exception as e:
        print(f"\n错误: {str(e)}")
        import traceback
        traceback.print_exc()
