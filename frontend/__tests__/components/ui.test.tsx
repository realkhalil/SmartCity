import { fireEvent, render, screen } from '@testing-library/react'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'

describe('ui primitives', () => {
  it('renders card compound components with custom classes', () => {
    render(
      <Card className="outer">
        <CardHeader>
          <CardTitle>Title</CardTitle>
          <CardDescription>Description</CardDescription>
        </CardHeader>
        <CardContent>Content</CardContent>
        <CardFooter>Footer</CardFooter>
      </Card>
    )

    expect(screen.getByText('Title')).toBeInTheDocument()
    expect(screen.getByText('Description')).toBeInTheDocument()
    expect(screen.getByText('Content')).toBeInTheDocument()
    expect(screen.getByText('Footer')).toBeInTheDocument()
  })

  it('renders buttons and handles clicks', () => {
    const onClick = jest.fn()
    render(<Button variant="outline" size="sm" onClick={onClick}>Save</Button>)

    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(onClick).toHaveBeenCalled()
    expect(buttonVariants({ variant: 'link', size: 'lg' })).toContain('underline')
  })

  it('renders button as child through Radix slot', () => {
    render(
      <Button asChild>
        <a href="/dashboard">Dashboard</a>
      </Button>
    )

    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/dashboard')
  })

  it('renders input, label and skeleton', () => {
    render(
      <div>
        <Label htmlFor="email">Email</Label>
        <Input id="email" placeholder="you@test.ma" className="field" />
        <Skeleton data-testid="loading" className="h-4" />
      </div>
    )

    expect(screen.getByLabelText('Email')).toHaveClass('field')
    expect(screen.getByTestId('loading')).toHaveClass('animate-pulse')
  })
})
