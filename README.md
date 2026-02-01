# Personalized Goal Forecasting (MVP)

Simple full-stack MVP (Spring Boot + React) that lets a user upload a transactions CSV and a goal, then forecasts whether the goal is on track and suggests actionable savings levers if not.

## Tech
- Backend: Spring Boot (Java 17), Maven
- Frontend: React + Vite

## Transactions CSV format
Header row required, columns:
- `date` (yyyy-MM-dd, MM/dd/yyyy, or dd/MM/yyyy)
- `amount` (positive inflow, negative outflow)
- `merchant`
- `category`
- `account`

## Run locally

### Backend
```
cd backend
mvn spring-boot:run
```
Server runs on `http://localhost:8080`.

### Frontend
```
cd frontend
npm install
npm run dev
```
App runs on `http://localhost:5173`.

## Usage
1. Start backend, then frontend.
2. Open the app in browser.
3. Upload a CSV of transactions and fill in goal details (amount, months to deadline, current savings, buffer).
4. Click Analyze to see:
	- On-track status (on_track, borderline, off_track)
	- Required monthly vs projected monthly
	- Savings capacity distribution (p10/p50/p90)
	- Forecasted balance at deadline
	- Suggestions with estimated monthly impact and rationale

## Notes
- Baseline uses last 3 months for savings capacity percentiles.
- Strong cut suggestions focus on discretionary categories only.
- Timeline and light income levers included.
