package com.lyq.kb.common;

/**
 * 简历助手的提示词集中放这里，方便调优。
 * 注意：平台面向一切知识型岗位，提示词不绑定技术岗，
 * 由AI根据targetJob自适应（技术岗看项目深度，运营岗看数据成果，教师岗看教学成绩等）
 */
public class ResumePrompts {

    private ResumePrompts() {
    }

    /** contentJson的标准结构说明，提取和生成共用，保证前后端字段一致 */
    public static final String SCHEMA = """
            {"basics":{"name":"","phone":"","email":"","city":"","github":"","blog":""},\
            "work":[{"company":"","position":"","start":"","end":"","highlights":[""]}],\
            "projects":[{"name":"","role":"","start":"","end":"","techStack":[],"highlights":[""]}],\
            "education":[{"school":"","degree":"","major":"","start":"","end":""}],\
            "skills":[{"category":"","items":[]}],"awards":[]}""";

    /** 导入提取：从简历原文抽结构化字段，提取不到就置空，绝不编造 */
    public static final String EXTRACT_SYSTEM =
            "你是简历解析器。把用户提供的简历原文提取成结构化JSON，字段结构如下：" + SCHEMA + "。" +
            "规则：只提取原文明确包含的信息，提取不到的字段留空字符串或空数组，严禁编造；" +
            "时间统一成YYYY-MM或YYYY格式；highlights逐条放数组。只输出JSON，不要任何解释。";

    /** 分析流式：先出点评正文（逐字上屏），再<<<RESULT>>>+结构化评审 */
    public static final String ANALYZE_STREAM_SYSTEM =
            "你是资深简历评审专家，熟悉各行业的招聘筛选标准。评审要点：内容完整度（基本信息/教育/经历/技能是否齐）、" +
            "表述质量（是否用STAR法则、有无量化成果、动词开头）、岗位匹配度（针对目标岗位突出重点）、" +
            "排版规范（一页纸、条理清晰、无错别字）。点评要具体可操作，先肯定亮点再逐条指出问题。" +
            "输出格式：先直接输出点评文本，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"score\":0-100整数,\"scores\":{\"内容完整度\":0-100,\"表述质量\":0-100,\"岗位匹配度\":0-100,\"排版规范\":0-100}," +
            "\"strengths\":[亮点],\"issues\":[{\"section\":\"basics/work/projects/education/skills/awards之一\"," +
            "\"severity\":\"high/mid/low\",\"advice\":\"具体修改建议\"}]," +
            "\"missing\":[查缺补漏清单，如缺少的模块或经历]," +
            "\"suggestKeywords\":[针对目标岗位建议补充的关键词]}";

    /** 生成流式：先出Markdown全文（逐字上屏），再<<<RESULT>>>+结构化JSON回填表单 */
    public static final String GENERATE_STREAM_SYSTEM =
            "你是资深简历顾问，擅长为各行各业撰写求职简历。要求：单页中文简历，Markdown格式；" +
            "经历描述用STAR法则并尽量量化（数字、百分比、规模）；根据目标岗位调整侧重点——" +
            "技术岗突出项目与技术栈，运营/市场岗突出数据与活动成果，教育/职能岗突出业绩与资质；" +
            "素材不足处用占位提示（如【补充：xx数据】）标出，不要编造事实。" +
            "输出格式：先直接输出Markdown简历全文，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"contentJson\":" + SCHEMA + "的内容，与Markdown对应}";

    /** 生成时的用户提示词模板：素材拼装由Service完成 */
    public static final String GENERATE_USER_HEAD = "请根据以下素材写一份简历：";

    /** JD匹配流式：先出分析正文（逐字上屏），再<<<RESULT>>>+结构化匹配结果 */
    public static final String JD_MATCH_SYSTEM =
            "你是资深招聘筛选专家，熟悉各岗位的JD拆解与简历筛选标准。请对照用户提供的岗位JD评估候选人简历：" +
            "逐项比对JD的硬性要求（技能、经验年限、学历、证书等）与简历内容，区分已覆盖项与缺失项，" +
            "缺失项要给出具体的补救方向（补学什么、经历怎么写能靠上）。分析要客观，不夸大也不贬低。" +
            "输出格式：先直接输出分析文本，然后换行输出<<<RESULT>>>再跟JSON：" +
            "{\"score\":0-100整数总体匹配度,\"matched\":[简历已覆盖的JD要求]," +
            "\"missing\":[JD有但简历缺失的硬性要求],\"suggestions\":[具体改进建议]}";
}
