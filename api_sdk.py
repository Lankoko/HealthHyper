"""
HealthHyper 中台 API SDK & 测试脚本
====================================
用途：
  1. 直接运行 `python api_sdk.py` → 逐个测试全部中台接口
  2. 复制函数体到 LangChain @tool 实现中 → 改参数来源即可上线

配置：修改下面三个变量即可
"""

import requests
from typing import Optional

# ═══════════════════════════════════════════
#  配置区（根据实际环境修改）
# ═══════════════════════════════════════════
BASE_URL = "http://localhost:8080"
API_KEY = "HealthHyperAiToolKey2026"
USER_ID = "3"


# ═══════════════════════════════════════════
#  通用 HTTP 封装（复制到你的 tools/__init__.py）
# ═══════════════════════════════════════════
def _headers():
    return {
        "X-Api-Key": API_KEY,
        "X-User-Id": USER_ID,
        "Content-Type": "application/json",
    }


def _get(path: str, params: dict = None) -> dict:
    resp = requests.get(f"{BASE_URL}{path}", headers=_headers(), params=params, timeout=10)
    return resp.json()


def _post(path: str, body: dict = None) -> dict:
    resp = requests.post(f"{BASE_URL}{path}", headers=_headers(), json=body or {}, timeout=10)
    return resp.json()


def _put(path: str, body: dict = None) -> dict:
    resp = requests.put(f"{BASE_URL}{path}", headers=_headers(), json=body or {}, timeout=10)
    return resp.json()


# ═══════════════════════════════════════════
#  Skill: user_profile（用户档案）
# ═══════════════════════════════════════════
def get_user_profile() -> str:
    """获取用户健康档案"""
    result = _get("/api/user/profile")
    data = result.get("data", {})
    return str(data)


def update_user_profile(
    gender: Optional[int] = None,
    birth_date: Optional[str] = None,
    height_cm: Optional[float] = None,
    weight_kg: Optional[float] = None,
    blood_type: Optional[str] = None,
    medical_history: Optional[str] = None,
    allergy_info: Optional[str] = None,
    lifestyle_info: Optional[str] = None,
) -> str:
    """更新用户健康档案中的一个或多个字段"""
    body = {}
    if gender is not None:          body["gender"] = gender
    if birth_date is not None:      body["birthDate"] = birth_date
    if height_cm is not None:       body["heightCm"] = height_cm
    if weight_kg is not None:       body["weightKg"] = weight_kg
    if blood_type is not None:      body["bloodType"] = blood_type
    if medical_history is not None: body["medicalHistory"] = medical_history
    if allergy_info is not None:    body["allergyInfo"] = allergy_info
    if lifestyle_info is not None:  body["lifestyleInfo"] = lifestyle_info
    result = _put("/api/user/profile", body)
    return "更新成功" if result.get("code") == 200 else str(result)


# ═══════════════════════════════════════════
#  Skill: physiological_analysis（生理分析）
# ═══════════════════════════════════════════
def get_current_physiological(metric: str = "all") -> str:
    """获取最新一条生理数据，metric: all|hr|spo2|bt"""
    result = _get("/api/vital/current", {"metric": metric})
    return str(result.get("data", {}))


def get_physiological_stats(metric: str = "all", days: int = 7) -> str:
    """获取最近N天生理数据统计（均值/最大/最小）"""
    result = _get("/api/vital/stats", {"metric": metric, "days": days})
    return str(result.get("data", {}))


# ═══════════════════════════════════════════
#  Skill: physiological_baseline（基线）
# ═══════════════════════════════════════════
def get_baseline(metric: str = "all") -> str:
    """获取生理基线，metric: all|hr|spo2|bt|hr_cv|sdann"""
    result = _get("/api/baseline", {"metric": metric})
    return str(result.get("data", {}))


def update_one_baseline(metric: str, value: float) -> str:
    """更新单个指标基线"""
    result = _put("/api/baseline", {"metric": metric, "value": value})
    return "更新成功" if result.get("code") == 200 else str(result)


def update_all_baseline(
    hr_base: float, spo2_base: float, bt_base: float,
    hr_cv_base: float, sdann_base: float,
) -> str:
    """一次性更新全部基线"""
    result = _put("/api/baseline/all", {
        "hrBase": hr_base,
        "spo2Base": spo2_base,
        "btBase": bt_base,
        "hrCvBase": hr_cv_base,
        "sdannBase": sdann_base,
    })
    return "更新成功" if result.get("code") == 200 else str(result)


# ═══════════════════════════════════════════
#  Skill: sleep_analysis（睡眠）
# ═══════════════════════════════════════════
def obtain_single_night_sleep() -> str:
    """获取最近一晚睡眠数据（含分期、生理指标、AI分析）"""
    result = _get("/api/sleep/latest")
    return str(result.get("data", {}))


def obtain_multi_night_sleep(days: int = 7) -> str:
    """获取最近N晚睡眠数据"""
    result = _get("/api/sleep/recent", {"days": days})
    return str(result.get("data", []))


def update_sleep_analysis(ai_analysis: str) -> str:
    """AI 回填最新一条睡眠记录的分析结论（无需传 ID）"""
    result = _put("/api/sleep/latest/analysis", {"aiAnalysis": ai_analysis})
    return "回填成功" if result.get("code") == 200 else str(result)


def get_sleep_plan() -> str:
    """获取当前睡眠改善计划"""
    result = _get("/api/sleep/plan")
    return str(result.get("data"))


def update_sleep_plan(items_json: str) -> str:
    """创建/更新睡眠改善计划"""
    result = _put("/api/sleep/plan", {"items_json": items_json})
    return "更新成功" if result.get("code") == 200 else str(result)


# ═══════════════════════════════════════════
#  Skill: medication_assistant（用药助手）
# ═══════════════════════════════════════════
def add_medication_plan(
    drug_name: str,
    dosage: str,
    frequency: str,
    time_slots: str,
    notes: str = "",
) -> str:
    """创建用药计划（日期由中台自动填充）"""
    result = _post("/api/medication/plan", {
        "drugName": drug_name,
        "dosage": dosage,
        "frequency": frequency,
        "timeSlots": time_slots,
        "notes": notes,
    })
    return "创建成功" if result.get("code") == 200 else str(result)


def obtain_medication_plan() -> str:
    """获取全部有效用药计划"""
    result = _get("/api/medication/plan/list")
    return str(result.get("data"))


def record_medication_taking(drug_name: str, time: str = "") -> str:
    """记录一次服药，time 格式 '2026-04-09 08:15:00'，不传则默认当前时间"""
    body = {"drugName": drug_name}
    if time:
        body["time"] = time
    result = _post("/api/medication/log", body)
    return "记录成功" if result.get("code") == 200 else str(result)


def obtain_medication_log(days: int = 3) -> str:
    """查看最近N天服药记录（含药品名、剂量等计划信息）"""
    result = _get("/api/medication/log", {"days": days})
    return str(result.get("data", []))


# ═══════════════════════════════════════════
#  Skill: daily_tracking（日常记录）
# ═══════════════════════════════════════════
def add_daily_record(log_type: str, content: str, extra_json: str = "{}") -> str:
    """添加日常记录，log_type: diet|exercise|mood|symptom|other"""
    result = _post("/api/daily", {
        "logType": log_type,
        "content": content,
        "extraJson": extra_json,
    })
    return "记录成功" if result.get("code") == 200 else str(result)


def get_daily_records(log_type: str = "", days: int = 7, limit: int = 20) -> str:
    """查询日常记录"""
    params = {"days": days, "limit": limit}
    if log_type:
        params["logType"] = log_type
    result = _get("/api/daily", params)
    return str(result.get("data", []))


# ═══════════════════════════════════════════
#  Skill: medical_report_analysis（医疗报告）
#  report_type: exam_report | health_analysis
# ═══════════════════════════════════════════
def generate_medical_report(
    report_type: str,
    report_title: str,
    report_content: str,
    chat_session_id: Optional[int] = None,
) -> str:
    """保存医疗报告/健康分析报告"""
    body = {
        "reportType": report_type,
        "reportTitle": report_title,
        "reportContent": report_content,
    }
    if chat_session_id is not None:
        body["chatSessionId"] = chat_session_id
    result = _post("/api/medical/report", body)
    return "保存成功" if result.get("code") == 200 else str(result)


# ═══════════════════════════════════════════
#  Skill: health_planning（健康计划）
# ═══════════════════════════════════════════
def create_health_plan(title: str, content: str, days: int = 7) -> str:
    """创建健康计划（status=0 待打卡）"""
    result = _post("/api/plan", {
        "title": title,
        "content": content,
        "days": days,
    })
    data = result.get("data", {})
    plan_id = data.get("id", "?")
    return f"计划创建成功，plan_id={plan_id}" if result.get("code") == 200 else str(result)


def get_health_plans(days: int = 7) -> str:
    """查询最近N天的健康计划"""
    result = _get("/api/plan", {"days": days})
    return str(result.get("data", []))


def checkin_health_plan(plan_id: int) -> str:
    """打卡（标记计划已完成）"""
    result = _put(f"/api/plan/{plan_id}/checkin")
    return "打卡成功" if result.get("code") == 200 else str(result)


# ═══════════════════════════════════════════
#  Skill: conversation_summary（对话摘要）
# ═══════════════════════════════════════════
def summarize_conversation(summary: str) -> str:
    """保存/更新对话摘要"""
    result = _post("/api/ai/summary", {"summary": summary})
    return "保存成功" if result.get("code") == 200 else str(result)


def get_conversation_memory() -> str:
    """获取最新对话摘要"""
    result = _get("/api/ai/summary/latest")
    return str(result.get("data"))


# ═══════════════════════════════════════════
#  测试入口：逐个跑全部接口
# ═══════════════════════════════════════════
def _test(name: str, fn, *args, **kwargs):
    print(f"\n{'─'*50}")
    print(f"▶ {name}")
    try:
        result = fn(*args, **kwargs)
        print(f"  ✅ {result[:200] if len(result) > 200 else result}")
    except Exception as e:
        print(f"  ❌ {e}")


if __name__ == "__main__":
    print("=" * 50)
    print("  HealthHyper 中台 API 全量测试")
    print(f"  BASE_URL = {BASE_URL}")
    print(f"  USER_ID  = {USER_ID}")
    print("=" * 50)

    # ── user_profile ──
    _test("get_user_profile", get_user_profile)
    _test("update_user_profile", update_user_profile,
          gender=1, height_cm=175.0, weight_kg=68.5,
          medical_history="无重大病史", lifestyle_info="久坐办公")

    # ── physiological_analysis ──
    _test("get_current_physiological", get_current_physiological, "all")
    _test("get_physiological_stats", get_physiological_stats, "hr", 7)

    # ── physiological_baseline ──
    _test("get_baseline", get_baseline, "all")
    _test("update_one_baseline(hr)", update_one_baseline, "hr", 72.5)
    _test("update_all_baseline", update_all_baseline,
          hr_base=72.5, spo2_base=97.0, bt_base=36.5,
          hr_cv_base=0.05, sdann_base=120.0)

    # ── sleep_analysis ──
    _test("obtain_single_night_sleep", obtain_single_night_sleep)
    _test("obtain_multi_night_sleep", obtain_multi_night_sleep, 3)
    _test("get_sleep_plan", get_sleep_plan)

    # ── medication_assistant ──
    _test("add_medication_plan", add_medication_plan,
          drug_name="布洛芬缓释胶囊", dosage="每次0.3g",
          frequency="每日2次", time_slots="08:00,20:00",
          notes="饭后服用")
    _test("obtain_medication_plan", obtain_medication_plan)
    _test("record_medication_taking", record_medication_taking, "布洛芬")
    _test("obtain_medication_log", obtain_medication_log, 3)

    # ── daily_tracking ──
    _test("add_daily_record(mood)", add_daily_record,
          "mood", "今天状态不错",
          '{"mood_score":4,"tags":["happy"]}')
    _test("get_daily_records(mood)", get_daily_records, "mood", 7)

    # ── medical_report_analysis ──
    _test("generate_medical_report", generate_medical_report,
          report_type="health_analysis",
          report_title="测试报告",
          report_content='{"summary":"测试用健康分析报告"}')

    # ── health_planning ──
    _test("create_health_plan", create_health_plan,
          title="SDK测试计划",
          content='[{"desc":"每天喝水2000ml","category":"diet"}]',
          days=7)
    _test("get_health_plans", get_health_plans, 7)

    # ── conversation_summary ──
    _test("summarize_conversation", summarize_conversation,
          '{"recent_topics":["SDK测试"],"key_findings":"测试通过"}')
    _test("get_conversation_memory", get_conversation_memory)

    print(f"\n{'='*50}")
    print("  全部测试完成")
    print(f"{'='*50}")
