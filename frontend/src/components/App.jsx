import React, { useMemo, useState } from 'react'
import { Upload, Target, TrendingUp, AlertCircle } from 'lucide-react'

function App() {
  const [file, setFile] = useState(null)
  const [goalText, setGoalText] = useState('Save $3000 in 10 months for a new laptop')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showMore, setShowMore] = useState(false)
  
  const preview = useMemo(() => parseGoalText(goalText), [goalText])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('goal', JSON.stringify({ goalText }))
      const res = await fetch('http://localhost:8080/api/forecast/analyze', {
        method: 'POST',
        body: form,
      })
      if (!res.ok) throw new Error('Request failed')
      const data = await res.json()
      setResult(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ 
      minHeight: '100vh', 
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      padding: '40px 20px',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
    }}>
      <div style={{ maxWidth: '960px', margin: '0 auto' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h1 style={{ fontSize: 42, fontWeight: 700, color: '#fff', marginBottom: 12, textShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
            Financial Goal Forecasting Advisor
          </h1>
          <p style={{ fontSize: 18, color: 'rgba(255,255,255,0.9)', maxWidth: 640, margin: '0 auto', lineHeight: 1.6 }}>
            Upload your transactions, set your goal, and get a transparent forecast with practical suggestions. No judgment—just options.
          </p>
        </div>

        {/* Step 1: Upload */}
        <div style={{ background: '#fff', borderRadius: 16, padding: 24, marginBottom: 20, boxShadow: '0 10px 40px rgba(0,0,0,0.15)' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ background: '#667eea', borderRadius: 12, width: 48, height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', marginRight: 16 }}>
              <Upload size={24} color="white" />
            </div>
            <h2 style={{ fontSize: 24, fontWeight: 600, color: '#1a1a1a', margin: 0 }}>1. Upload Transactions</h2>
          </div>
          <p style={{ color: '#666', marginBottom: 16, fontSize: 15 }}>CSV with headers: date, amount, merchant, category, account.</p>
          <label style={{ 
            display: 'block', padding: '16px 24px', background: file ? '#f0fdf4' : '#f8fafc', border: file ? '2px solid #10b981' : '2px dashed #cbd5e1', borderRadius: 12, cursor: 'pointer', textAlign: 'center'
          }}>
            <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files[0])} style={{ display: 'none' }} />
            <span style={{ color: file ? '#10b981' : '#64748b', fontSize: 15, fontWeight: 500 }}>{file ? `✓ ${file.name}` : 'Choose file'}</span>
          </label>
        </div>

        {/* Step 2: Set Goal */}
        <div style={{ background: '#fff', borderRadius: 16, padding: 24, marginBottom: 20, boxShadow: '0 10px 40px rgba(0,0,0,0.15)' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ background: '#667eea', borderRadius: 12, width: 48, height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', marginRight: 16 }}>
              <Target size={24} color="white" />
            </div>
            <h2 style={{ fontSize: 24, fontWeight: 600, color: '#1a1a1a', margin: 0 }}>2. Set Your Goal</h2>
          </div>
          <p style={{ color: '#666', marginBottom: 16, fontSize: 15 }}>Describe your goal in plain English</p>

          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
              {[
                'Save $5,000 by June 2026 for a trip',
                'Put aside $1.2k in 6 months for a laptop',
                '$800 within 90 days to build an emergency buffer',
                '$2,500 by 12/31/2026 for moving costs'
              ].map((ex, i) => (
                <button key={i} type="button" onClick={() => setGoalText(ex)}
                  style={{ padding: '8px 16px', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: 13, color: '#475569', cursor: 'pointer' }}
                >{ex}</button>
              ))}
            </div>
            <textarea className="input" rows={3} value={goalText} onChange={(e) => setGoalText(e.target.value)} placeholder="e.g., Save $5,000 by June 2026 for a vacation"
              style={{ width: '100%', minHeight: 100, padding: 16, fontSize: 15, border: '2px solid #e2e8f0', borderRadius: 12, fontFamily: 'inherit' }}
            />
          </div>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12, fontSize: 14, color: '#64748b' }}>
            <strong>Tip:</strong> Include an amount (e.g., $1200 or 1.2k) and a timeframe (e.g., in 6 months or by June 2026).
          </div>

          {/* Preview chips */}
          <div style={{ marginTop: 16, display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            {preview?.amount && (
              <div style={{ flex: 1, minWidth: 150, padding: 16, background: '#fefce8', borderRadius: 12, border: '1px solid #fde047' }}>
                <div style={{ fontSize: 12, color: '#854d0e', fontWeight: 600, marginBottom: 4 }}>AMOUNT</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: '#854d0e' }}>${Number(preview.amount).toLocaleString()}</div>
              </div>
            )}
            {preview?.months && (
              <div style={{ flex: 1, minWidth: 150, padding: 16, background: '#eff6ff', borderRadius: 12, border: '1px solid #93c5fd' }}>
                <div style={{ fontSize: 12, color: '#1e40af', fontWeight: 600, marginBottom: 4 }}>TIMEFRAME</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: '#1e40af' }}>{preview.months} months</div>
              </div>
            )}
            {preview?.amount && preview?.months && (
              <div style={{ flex: 1, minWidth: 150, padding: 16, background: '#f0fdf4', borderRadius: 12, border: '1px solid #86efac' }}>
                <div style={{ fontSize: 12, color: '#166534', fontWeight: 600, marginBottom: 4 }}>REQUIRED</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: '#166534' }}>${(Number(preview.amount) / preview.months).toFixed(2)}/mo</div>
              </div>
            )}
          </div>

          <button className="btn btn-primary" type="button" onClick={handleSubmit} disabled={loading || (!file && !result)}
            style={{ width: '100%', marginTop: 16, padding: 18, background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: '#fff', borderRadius: 12, fontSize: 16, fontWeight: 600 }}
          >Analyze with uploaded CSV</button>
        </div>

        {/* Step 3: Results */}
        <div style={{ background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 10px 40px rgba(0,0,0,0.15)' }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ background: '#667eea', borderRadius: 12, width: 48, height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', marginRight: 16 }}>
              <TrendingUp size={24} color="white" />
            </div>
            <h2 style={{ fontSize: 24, fontWeight: 600, color: '#1a1a1a', margin: 0 }}>3. Your Forecast</h2>
          </div>
          {loading && <p>Analyzing…</p>}
          {error && <p style={{ color: 'red' }}>{error}</p>}
          {!loading && !result && <p>Run an analysis to see your forecast and suggestions.</p>}
          {result && (
            <div>
              {result.status === 'on_track' ? (
                <div style={{ background: '#f0fdf4', border: '2px solid #86efac', borderRadius: 12, padding: 16, marginBottom: 16 }}>
                  <p style={{ margin: 0, color: '#166534', fontWeight: 600 }}>You're on track for this goal.</p>
                </div>
              ) : (
                <div style={{ background: '#fef2f2', border: '2px solid #fecaca', borderRadius: 12, padding: 16, marginBottom: 16, display: 'flex', alignItems: 'start', gap: 12 }}>
                  <AlertCircle size={24} color="#dc2626" style={{ flexShrink: 0 }} />
                  <div>
                    <div style={{ fontWeight: 600, color: '#991b1b', marginBottom: 8 }}>
                      {result.monthlyGap ? `You're currently ~$${result.monthlyGap}/month short of your goal` : `You're off-track for this goal`}
                    </div>
                    <div style={{ color: '#991b1b' }}>
                      Here are some specific ways to close the gap.
                    </div>
                  </div>
                </div>
              )}

              {result.status !== 'on_track' && (
                <div>
                  <h3 style={{ fontSize: 18, fontWeight: 600, color: '#1a1a1a', marginBottom: 12 }}>Personalized Suggestions</h3>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 12 }}>
                    {(showMore ? (result.suggestions || []) : (result.suggestions || []).slice(0, 3)).map((s, idx) => (
                      <SuggestionCard key={idx} s={s} />
                    ))}
                  </div>
                  {(result.suggestions && result.suggestions.length > 3) && (
                    <div style={{ marginTop: 12 }}>
                      <button className="btn btn-secondary" type="button" onClick={() => setShowMore(v => !v)}>
                        {showMore ? 'Show fewer suggestions' : 'Show more suggestions'}
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function Card({ title, children }) {
  return (
    <div className="card">
      <div className="card-title">{title}</div>
      <div>{children}</div>
    </div>
  )
}

function StatusBadge({ status }) {
  const map = {
    on_track: { label: 'On Track', cls: 'badge badge--on' },
    borderline: { label: 'Borderline', cls: 'badge badge--borderline' },
    off_track: { label: 'Off Track', cls: 'badge badge--off' },
  }
  const s = map[status] || { label: String(status), cls: 'badge' }
  return <span className={s.cls}>{s.label}</span>
}

function SuggestionCard({ s }) {
  return (
    <div className="suggestion-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div style={{ fontWeight: 600 }}>{s.title}</div>
        <div className="suggestion-impact">+${s.impactPerMonth}/mo</div>
      </div>
      <div style={{ marginTop: 6 }}>{s.action}</div>
      <small className="suggestion-meta" style={{ display: 'block', marginTop: 6 }}>{s.rationale}</small>
      {s.leverType === 'timeline' && s.newRequiredMonthly && (
        <div style={{ marginTop: 6, background: '#f7f7f7', padding: 8, borderRadius: 6 }}>
          Timeline option: {s.newMonthsToDeadline} months → ${s.newRequiredMonthly}/mo
        </div>
      )}
    </div>
  )
}

export default App
 
function parseGoalText(text) {
  if (!text || !text.trim()) return null
  const s = text.trim()

  const amount = parseAmount(s)
  let months = null

  const deadlineAbs = parseAbsoluteDeadline(s)
  if (deadlineAbs) {
    months = monthsBetween(new Date(), deadlineAbs)
  } else {
    months = parseRelativeMonths(s)
  }

  if (!amount || !months || months <= 0) return null
  return { amount: Number(amount), months }
}

function parseAmount(s) {
  const m1 = s.match(/\$\s*((?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\.[0-9]{1,2})?)(?![0-9])/)
  if (m1) return Number(m1[1].replace(/,/g, ''))
  const m2 = s.match(/([0-9]+(?:\.[0-9]{1,2})?)\s*(usd|dollars|bucks)/i)
  if (m2) return Number(m2[1])
  const m3 = s.match(/([0-9]+(?:\.[0-9]+)?)\s*([kmb])/i)
  if (m3) {
    const n = Number(m3[1])
    const suf = m3[2].toLowerCase()
    if (suf === 'k') return n * 1000
    if (suf === 'm') return n * 1_000_000
    if (suf === 'b') return n * 1_000_000_000
  }
  return null
}

function parseRelativeMonths(s) {
  const m = s.match(/(?:in|within)\s+([0-9]{1,4})\s*(day|days|month|months|year|years)/i)
  if (m) {
    const n = Number(m[1])
    const unit = m[2].toLowerCase()
    if (unit.startsWith('day')) return Math.max(1, Math.ceil(n / 30))
    if (unit.startsWith('month')) return Math.max(1, n)
    if (unit.startsWith('year')) return Math.max(1, n * 12)
  }
  return null
}

function parseAbsoluteDeadline(s) {
  const iso = s.match(/by\s+([0-9]{4}-[0-9]{2}-[0-9]{2})/i)
  if (iso) return new Date(iso[1])
  const slash = s.match(/by\s+([0-9]{1,2}\/[0-9]{1,2}\/[0-9]{2,4})/i)
  if (slash) {
    const parts = slash[1].split('/')
    const mm = Number(parts[0]) - 1
    const dd = Number(parts[1])
    let yy = Number(parts[2])
    if (yy < 100) yy += 2000
    return new Date(yy, mm, dd)
  }
  const monthName = s.match(/by\s+(January|February|March|April|May|June|July|August|September|October|November|December)\s*(?:([0-9]{1,2})\s*,?\s*)?([0-9]{4})?/i)
  if (monthName) {
    const monthMap = {
      January:0, February:1, March:2, April:3, May:4, June:5,
      July:6, August:7, September:8, October:9, November:10, December:11
    }
    const m = monthMap[monthName[1]]
    const day = monthName[2] ? Number(monthName[2]) : null
    const year = monthName[3] ? Number(monthName[3]) : new Date().getFullYear()
    const d = day ?? new Date(year, m + 1, 0).getDate()
    return new Date(year, m, d)
  }
  const endOf = s.match(/by\s+end\s+of\s+(January|February|March|April|May|June|July|August|September|October|November|December)\s*([0-9]{4})?/i)
  if (endOf) {
    const monthMap = {
      January:0, February:1, March:2, April:3, May:4, June:5,
      July:6, August:7, September:8, October:9, November:10, December:11
    }
    const m = monthMap[endOf[1]]
    const year = endOf[2] ? Number(endOf[2]) : new Date().getFullYear()
    const d = new Date(year, m + 1, 0).getDate()
    return new Date(year, m, d)
  }
  const nextMonth = s.match(/by\s+next\s+(January|February|March|April|May|June|July|August|September|October|November|December)/i)
  if (nextMonth) {
    const monthMap = {
      January:0, February:1, March:2, April:3, May:4, June:5,
      July:6, August:7, September:8, October:9, November:10, December:11
    }
    const m = monthMap[nextMonth[1]]
    let year = new Date().getFullYear()
    const now = new Date()
    if (m <= now.getMonth()) year += 1
    const d = new Date(year, m + 1, 0).getDate()
    return new Date(year, m, d)
  }
  return null
}

function monthsBetween(start, end) {
  const s = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const e = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  let months = (e.getFullYear() - s.getFullYear()) * 12 + (e.getMonth() - s.getMonth())
  if (e.getDate() >= s.getDate()) months += 1 // round up
  return Math.max(1, months)
}
