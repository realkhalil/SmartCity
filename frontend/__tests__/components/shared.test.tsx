import { fireEvent, render, screen } from '@testing-library/react'
import { AlertTriangle, Home } from 'lucide-react'
import { BrandMark } from '@/components/shared/Brand'
import { DashboardPageHeader } from '@/components/shared/DashboardPageHeader'
import { EmptyState } from '@/components/shared/EmptyState'
import { SectionHeading } from '@/components/shared/SectionHeading'
import { Toast } from '@/components/shared/Toast'

describe('shared components', () => {
  it('renders brand mark in normal and compact modes', () => {
    const { rerender } = render(<BrandMark />)
    expect(screen.getByText('UrbanOps')).toBeInTheDocument()

    rerender(<BrandMark compact />)
    expect(screen.queryByText('UrbanOps')).not.toBeInTheDocument()
  })

  it('renders section heading optional fields', () => {
    render(<SectionHeading eyebrow="Live" title="Dashboard" description="Overview" align="center" />)

    expect(screen.getByText('Live')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Overview')).toBeInTheDocument()
  })

  it('renders dashboard header with icon and actions', () => {
    render(
      <DashboardPageHeader
        eyebrow="Admin"
        title="Incidents"
        description="Manage reports"
        icon={Home}
        actions={<button>Refresh</button>}
      />
    )

    expect(screen.getByText('Admin')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Incidents' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument()
  })

  it('renders empty state with action', () => {
    render(
      <EmptyState
        icon={AlertTriangle}
        title="No incidents"
        description="Nothing to show"
        action={<button>Create</button>}
      />
    )

    expect(screen.getByText('No incidents')).toBeInTheDocument()
    expect(screen.getByText('Nothing to show')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument()
  })

  it('closes toast by button and timeout', () => {
    jest.useFakeTimers()
    const onClose = jest.fn()
    render(<Toast message="Saved" type="success" onClose={onClose} />)

    expect(screen.getByText(/Saved/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button'))
    expect(onClose).toHaveBeenCalledTimes(1)

    jest.advanceTimersByTime(4000)
    expect(onClose).toHaveBeenCalledTimes(2)
    jest.useRealTimers()
  })
})
