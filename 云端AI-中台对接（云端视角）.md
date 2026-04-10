云端AI与中台对接（云端AI视角）

云端AI与中台交互需要SKILL.md说明文件，tools.json索引文件和xxx.py工具实现代码，在云端AI实现的工程目录中体现为：

- healthHyper

|- skills

| |- skills_library

| | |- xxx
| | | |- SKILL.md
| | | |_ tools.json

|_ tools
| |- init.py
| |_ xxx.py

healthHyper：工程目录

skills：工具目录与加载实现

skills_library：说明和索引

tools：工具代码实现

SKILL.md：skill说明内容

tools.json：工具索引
init.py：包文件
xxx.py：skill附属工具

SKILL.md中，声明可调用的工具，如：

// user_profile

可使用工具：

- get_user_profile：获得系统提供的用户基本信息
- update_user_profile：更新用户基本档案信息中的一个或多个字段

参数：

- gender：性别
- birth_date：出生日期
- lifestyle：生活习惯

返回值：更新成功与否

tools.json中建立可使用的工具索引：

// user_profile
{

  "tools": ["get_user_profile", "update_user_profile"]

}

user_profile.py工具实现：

@tool

def get_user_profile():

  “”” 获取用户信息 “””

  // 获取过程代码，此处标记为①，结合下面①标记

  return f”用户基本信息为：...”

@tool

def update_user_profile(gender: str, birth_date: str, lifestyle: str) -> str:

  “”” 更新用户信息 “””

  // 更新过程代码，此处标记为②，结合下面②标记

  return f”用户基本信息更新成功.”

中台提供对应接口说明，如：

// user_profile

get_user_profile

URL

http://192.168.16.34:8080/get_user_profile

Method

GET

Head(所有头部信息明确说明)

X-Api-Key: HealthHyperAiToolKey2026

Body

(空)

Respone

{

  “gender”: “男”,

  “birth_date”: “2000-10-11”,

  “lifestyle”: “无不良嗜好”

}

// user_profile

update_user_profile

URL

http://192.168.16.34:8080/update_user_profile

Method

POST

Head(所有头部信息明确说明)

X-Api-Key: HealthHyperAiToolKey2026

Body

{

  “gender”: “男”,

  “birth_date”: “2000-10-11”,

  “lifestyle”: “晚睡”

}

Respone

{

  “code”: “Success”

}

中台可以按照python写好测试程序同接口交付给云端AI，如：

// user_profile

// get_user_profile 标记①

import requests

# 接口信息

url = "http://192.168.16.34:8080/get_user_profile"

headers = {

    "X-Api-Key": "HealthHyperAiToolKey2026"

}

try:

    # 发送 GET 请求

    response = requests.get(url, headers=headers)

    if response.status_code == 200:

        data = response.json()

        print("\n✅ 接口调用成功！")

        print("性别:", data.get("gender"))

        print("生日:", data.get("birth_date"))

        print("生活习惯:", data.get("lifestyle"))

    else:

        print("\n❌ 接口调用失败")

except Exception as e:

    print("❌ 请求出错:", e)

// user_profile

// update_user_profile 标记②

import requests

# 接口信息

url = "http://192.168.16.34:8080/update_user_profile"

headers = {

    "X-Api-Key": "HealthHyperAiToolKey2026",

    "Content-Type": "application/json"  # POST 接口必须加这个头

}

# 要更新的数据

body = {

    "gender": "男",

    "birth_date": "2000-10-11",

    "lifestyle": "晚睡"

}

try:

    # 发送 POST 请求

    response = requests.post(url, json=body, headers=headers)

    if data.get("code") == "Success":

        print("\n✅ 用户信息更新成功！")

    else:

        print("\n❌ 更新失败")

except Exception as e:

    print("❌ 请求出错:", e)