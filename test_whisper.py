import whisper

print("正在加载Whisper base模型...")
model = whisper.load_model("base")
print("✅ Whisper base模型加载成功！")
print(f"模型类型：{type(model)}")
print("Whisper环境准备完成！")
