import { Bus, Construction, MapPin, Trash2 } from 'lucide-react'
import {
  getCategoryDisplayIcon,
  getCategoryIconComponent,
  isBrokenEmojiIcon,
} from '@/lib/categoryIcons'

describe('category icon helpers', () => {
  it('detects empty and mojibake icon values', () => {
    expect(isBrokenEmojiIcon()).toBe(true)
    expect(isBrokenEmojiIcon('   ')).toBe(true)
    expect(isBrokenEmojiIcon('Ã°Å¸Å¡')).toBe(true)
    expect(isBrokenEmojiIcon('\uFFFD')).toBe(true)
    expect(isBrokenEmojiIcon('🌳')).toBe(false)
  })

  it('maps known category names to Lucide icons', () => {
    expect(getCategoryIconComponent('Transport')).toBe(Bus)
    expect(getCategoryIconComponent(' dechets ')).toBe(Trash2)
    expect(getCategoryIconComponent('Voirie')).toBe(Construction)
    expect(getCategoryIconComponent('Unknown')).toBe(MapPin)
  })

  it('uses mapped display icons when db icon is valid or broken', () => {
    expect(getCategoryDisplayIcon('Transport', '🚌')).toBe(Bus)
    expect(getCategoryDisplayIcon('Unknown', 'Ã°bad')).toBe(MapPin)
  })
})
