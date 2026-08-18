// 简历A4页HTML生成器：用户端预览/导出PDF、管理员审阅详情共用一套排版
// content是jsonresume风格对象（basics/work/projects/education/skills/awards）

/** 转义用户输入，拼HTML防注入 */
const esc = s => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

/**
 * 拼一个独立的A4简历页：预览嵌iframe、导出在新窗口打印，
 * 新窗口里只有简历本身，不会把应用页面带进PDF
 */
export function buildResumeHtml(content, targetJob) {
  const c = content || {}
  const b = c.basics || {}
  const rg = (s, e) => [s, e].filter(Boolean).join(' ~ ')
  const contacts = [
    ['电话', b.phone], ['邮箱', b.email], ['城市', b.city], ['主页', b.github || b.blog]
  ].filter(x => x[1]).map(x => `<div><span>${x[0]}</span>${esc(x[1])}</div>`).join('')
  const ln = (left, mid, date) =>
    `<div class="ln"><b>${esc(left)}</b>${mid ? `<span>${esc(mid)}</span>` : ''}${date ? `<em>${esc(date)}</em>` : ''}</div>`
  const lis = arr => (arr || []).filter(h => h && h.trim())
    .map(h => `<li>${esc(h)}</li>`).join('')

  const education = c.education || []
  const work = c.work || []
  const projects = c.projects || []
  const skills = c.skills || []
  const awards = c.awards || []

  const secs = []
  if (education.length) secs.push(`<section class="sec"><h3>教育经历</h3>${
    education.map(e => `<div class="item">${ln(e.school, [e.degree, e.major].filter(Boolean).join(' · '), rg(e.start, e.end))}</div>`).join('')
  }</section>`)
  if (work.length) secs.push(`<section class="sec"><h3>工作经历</h3>${
    work.map(w => `<div class="item">${ln(w.company, w.position, rg(w.start, w.end))}${lis(w.highlights) ? `<ul>${lis(w.highlights)}</ul>` : ''}</div>`).join('')
  }</section>`)
  if (projects.length) secs.push(`<section class="sec"><h3>项目 / 实践经历</h3>${
    projects.map(p => `<div class="item">${ln(p.name, p.role, rg(p.start, p.end))}${
      p.techStack && p.techStack.length ? `<div class="tech"><b>关键词：</b>${esc(p.techStack.join('、'))}</div>` : ''}${
      lis(p.highlights) ? `<ul>${lis(p.highlights)}</ul>` : ''}</div>`).join('')
  }</section>`)
  if (skills.length) secs.push(`<section class="sec skills"><h3>技能 / 专长</h3>${
    skills.map(s => `<div class="row"><b>${esc(s.category || '其他')}：</b>${esc((s.items || []).filter(Boolean).join('、'))}</div>`).join('')
  }</section>`)
  if (awards.length) secs.push(`<section class="sec"><h3>荣誉奖项</h3><ul class="awards">${lis(awards)}</ul></section>`)

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>${esc(b.name || '简历')}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  @page { size: A4; margin: 12mm 14mm; }
  body { font-family: "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", sans-serif; color: #2b2b2b;
         background: #e9eaec; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .page { width: 210mm; min-height: 296mm; margin: 0 auto; background: #fff; padding: 14mm 16mm; }
  .hd { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px;
        border-bottom: 3px solid #2b5fad; padding-bottom: 12px; }
  .name { font-size: 30px; font-weight: 700; color: #1f2d3d; letter-spacing: 4px; }
  .job { margin-top: 6px; font-size: 13px; color: #2b5fad; font-weight: 600; letter-spacing: 1px; }
  .hd-contact { text-align: right; font-size: 12px; color: #333; line-height: 1.9; }
  .hd-contact span { color: #2b5fad; margin-right: 6px; }
  .sec { margin-top: 15px; }
  .sec h3 { font-size: 15px; color: #2b5fad; letter-spacing: 3px; border-bottom: 1px solid #d9e0ec;
            padding-bottom: 4px; margin-bottom: 9px; }
  .sec h3::before { content: ""; display: inline-block; width: 4px; height: 14px; background: #2b5fad;
                    margin-right: 8px; vertical-align: -1px; }
  .item { margin-bottom: 9px; }
  .ln { display: flex; align-items: baseline; gap: 10px; }
  .ln b { font-size: 13.5px; color: #222; }
  .ln span { font-size: 12.5px; color: #4a4f57; }
  .ln em { margin-left: auto; font-style: normal; font-size: 12px; color: #8a8f99; white-space: nowrap; }
  .item ul, .awards { margin: 4px 0 0 18px; }
  .item li, .awards li { font-size: 12.5px; line-height: 1.75; color: #333; }
  .tech { font-size: 12px; color: #4a4f57; margin-top: 3px; }
  .tech b { color: #2b5fad; font-weight: 600; }
  .skills .row { font-size: 12.5px; line-height: 2; color: #333; }
  .skills .row b { color: #222; }
  @media print { body { background: #fff; } .page { width: auto; min-height: 0; padding: 0; } }
</style>
</head>
<body><div class="page">
  <header class="hd">
    <div>
      <div class="name">${esc(b.name || '未填写姓名')}</div>
      ${targetJob ? `<div class="job">求职意向：${esc(targetJob)}</div>` : ''}
    </div>
    <div class="hd-contact">${contacts}</div>
  </header>
  ${secs.join('\n')}
</div></body></html>`
}
