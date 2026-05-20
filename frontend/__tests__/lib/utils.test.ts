import { cn, getSeverityColor, getSeverityLabel, getStatusLabel } from '@/lib/utils'

describe('utils', () => {
  it('merges conditional Tailwind classes', () => {
    expect(cn('px-2', false && 'hidden', 'px-4')).toContain('px-4')
    expect(cn('text-sm', 'font-bold')).toBe('text-sm font-bold')
  })

  it.each([
    ['CRITICAL', '#ef4444'],
    ['HIGH', '#ef4444'],
    ['MED', '#f59e0b'],
    ['MEDIUM', '#f59e0b'],
    ['LOW', '#22c55e'],
    ['UNKNOWN', '#7a8899'],
  ])('returns severity color for %s', (severity, color) => {
    expect(getSeverityColor(severity)).toBe(color)
  })

  it.each([
    ['CRITICAL', 'Critique'],
    ['HIGH', 'Ã‰levÃ©'],
    ['MED', 'Moyen'],
    ['MEDIUM', 'Moyen'],
    ['LOW', 'Faible'],
    ['OTHER', 'OTHER'],
  ])('returns severity label for %s', (severity, label) => {
    expect(getSeverityLabel(severity)).toBe(label)
  })

  it.each([
    ['open', 'Ouvert'],
    ['in_progress', 'En cours'],
    ['resolved', 'RÃ©solu'],
    ['unknown', 'unknown'],
  ])('returns status label for %s', (status, label) => {
    expect(getStatusLabel(status)).toBe(label)
  })
})
