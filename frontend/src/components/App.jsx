import React, { useState } from 'react'

function App() {
  const [file, setFile] = useState(null)
  const [goal, setGoal] = useState({ targetAmount: 3000, monthsToDeadline: 10, currentSavings: 0, buffer: 0, protectedCategories: [] })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('goal', JSON.stringify(goal))
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
    <div style={{ fontFamily: 'system-ui', maxWidth: 800, margin: '24px auto', padding: 16 }}>
      <h1>Personalized Goal Forecasting</h1>
      <p>Upload transactions CSV and enter goal details to see forecast and suggestions.</p>

      <form onSubmit={handleSubmit} style={{ display: 'grid', gap: 12 }}>
        <div>
          <label>Transactions CSV:</label>
          <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files[0])} required />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>
            <label>Target Amount ($)</label>
            <input type="number" value={goal.targetAmount} onChange={(e) => setGoal({ ...goal, targetAmount: Number(e.target.value) })} />
          </div>
          <div>
            <label>Months to Deadline</label>
            <input type="number" value={goal.monthsToDeadline} onChange={(e) => setGoal({ ...goal, monthsToDeadline: Number(e.target.value) })} />
          </div>
          <div>
            <label>Current Savings ($)</label>
            <input type="number" value={goal.currentSavings} onChange={(e) => setGoal({ ...goal, currentSavings: Number(e.target.value) })} />
          </div>
          <div>
            <label>Buffer Requirement ($)</label>
            <input type="number" value={goal.buffer} onChange={(e) => setGoal({ ...goal, buffer: Number(e.target.value) })} />
          </div>
        </div>
        <button type="submit" disabled={loading || !file}>Analyze</button>
      </form>

      {loading && <p>Analyzing…</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {result && (
        <div style={{ marginTop: 24 }}>
          <h2>Forecast</h2>
          <p>
            Status: <b>{result.status}</b><br/>
            Required Monthly: ${result.requiredMonthly}<br/>
            Projected Monthly to Goal: ${result.projectedMonthlyToGoal}<br/>
            Savings Capacity p50: ${result.p50} (p10 ${result.p10}, p90 ${result.p90})<br/>
            Forecasted Balance at Deadline (p50): ${result.forecastedBalanceAtDeadlineP50}
          </p>

          {result.suggestions && result.suggestions.length > 0 ? (
            <div>
              <h3>Suggestions</h3>
              <ul>
                {result.suggestions.map((s, idx) => (
                  <li key={idx} style={{ marginBottom: 12 }}>
                    <b>{s.title}</b> — Impact: ${s.impactPerMonth}
                    <div>{s.action}</div>
                    <small style={{ color: '#555' }}>{s.rationale}</small>
                    {s.leverType === 'timeline' && s.newRequiredMonthly && (
                      <div style={{ marginTop: 4 }}>
                        If moved to {s.newMonthsToDeadline} months, needed per month becomes ${s.newRequiredMonthly}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p>You're on track! Here are optional optimization tips above.</p>
          )}
        </div>
      )}
    </div>
  )
}

export default App
