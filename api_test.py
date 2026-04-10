import requests
import json

def chat_stream_test():
    url = "http://localhost:8000/api/v1/chat/stream"

    print("=" * 60)
    print("流式对话接口测试工具")
    print("=" * 60)

    user_id = input("请输入 user_id（直接回车留空）：").strip()
    thread_id = input("请输入 thread_id（直接回车留空）：").strip()

    print("\n初始化完成，输入 exit 退出，输入 switch 切换用户ID和对话ID\n")

    while True:
        text = input("\n你：").strip()

        if text.lower() == "exit":
            print("退出测试")
            break

        if text.lower() == "switch":
            print("\n--- 切换用户信息 ---")
            user_id = input("请输入新的 user_id（回车不修改）：").strip() or user_id
            thread_id = input("请输入新的 thread_id（回车不修改）：").strip() or thread_id
            print("切换成功！")
            continue

        if not text:
            print("消息不能为空，请重新输入")
            continue

        image_url = input("输入 image_url（直接回车留空）：").strip()

        payload = {
            "user_id": user_id,
            "thread_id": thread_id,
            "text": text,
            "image_url": image_url
        }

        print("\n助手：", end="", flush=True)

        try:
            with requests.post(url, json=payload, stream=True, timeout=120) as resp:
                for line in resp.iter_lines():
                    if not line:
                        continue

                    line = line.decode("utf-8").strip()

                    # 处理 event: status
                    if line.startswith("event: status"):
                        print("\n[状态] 正在处理中...", end="", flush=True)
                        continue

                    # 处理 data: 内容
                    if line.startswith("data:"):
                        data_str = line.replace("data:", "").strip()
                        try:
                            data = json.loads(data_str)
                        except:
                            continue

                        # 关键修复：你的字段是 content 不是 token！
                        if "content" in data:
                            print(data["content"], end="", flush=True)

                        # 结束标志
                        if data == {}:
                            print("\n[状态] 生成完成\n")

        except Exception as e:
            print(f"\n请求异常：{str(e)}")

if __name__ == "__main__":
    chat_stream_test()