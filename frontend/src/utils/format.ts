import dayjs from 'dayjs'

export function formatDate(date: string | Date, format = 'YYYY-MM-DD'): string {
  return dayjs(date).format(format)
}

export function formatDateTime(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

export function formatMoney(amount: number): string {
  return `¥${amount.toFixed(2)}`
}

export function formatHours(hours: number): string {
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  if (h === 0) return `${m}分钟`
  if (m === 0) return `${h}小时`
  return `${h}小时${m}分钟`
}

export function formatMonth(year: number, month: number): string {
  return `${year}年${month}月`
}

export function getCurrentYearMonth(): { year: number; month: number } {
  const now = dayjs()
  return {
    year: now.year(),
    month: now.month() + 1
  }
}

export function getMonthRange(year: number, month: number): { start: string; end: string } {
  const start = dayjs(`${year}-${month}-01`).format('YYYY-MM-DD')
  const end = dayjs(start).endOf('month').format('YYYY-MM-DD')
  return { start, end }
}
