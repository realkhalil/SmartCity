import { render, screen } from '@testing-library/react'
import { SeverityBadge } from '@/components/shared/SeverityBadge'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Badge, badgeVariants } from '@/components/ui/badge'

describe('badge components', () => {
  it('renders severity labels with color styles', () => {
    render(<SeverityBadge severity="HIGH" />)

    const badge = screen.getByText('Ã‰levÃ©')
    expect(badge).toBeInTheDocument()
    expect(badge).toHaveStyle({ color: '#ef4444' })
  })

  it('renders compact severity badges', () => {
    render(<SeverityBadge severity="LOW" size="sm" />)

    expect(screen.getByText('Faible')).toHaveClass('text-[10px]')
  })

  it.each([
    ['OPEN', 'ðŸ”´ Ouvert'],
    ['in_progress', 'ðŸŸ¡ En cours'],
    ['resolved', 'âœ… RÃ©solu'],
    ['unexpected', 'ðŸ”´ Ouvert'],
  ])('normalizes status %s', (status, label) => {
    render(<StatusBadge status={status} size="sm" />)

    expect(screen.getByText(label)).toBeInTheDocument()
  })

  it('renders generic badge variants', () => {
    render(<Badge variant="success" className="custom">Actif</Badge>)

    expect(screen.getByText('Actif')).toHaveClass('custom')
    expect(badgeVariants({ variant: 'danger' })).toContain('text-red')
  })
})
