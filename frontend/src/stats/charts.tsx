import { Box, Card, CardContent, Typography, useTheme } from '@mui/material'
import type { ReactNode } from 'react'
import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts'
import { compact } from './periods'
import type { Bucket } from './periods'

export function ChartCard({ title, subtitle, children }: { title: string; subtitle?: ReactNode; children: ReactNode }) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="subtitle2" component="h2">{title}</Typography>
        {subtitle && <Typography variant="caption" color="text.secondary">{subtitle}</Typography>}
        <Box sx={{ mt: 1.5 }}>{children}</Box>
      </CardContent>
    </Card>
  )
}

export function EmptyChart({ text }: { text: string }) {
  return (
    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>{text}</Typography>
  )
}

/* the theme colors recharts needs as plain values so both modes stay readable */
function useChartTheme() {
  const { palette } = useTheme()
  return {
    bar: palette.primary.main,
    grid: palette.divider,
    tick: { fill: palette.text.secondary, fontSize: 12 },
    cursor: { fill: palette.action.hover },
    tooltip: {
      contentStyle: {
        backgroundColor: palette.background.paper,
        border: `1px solid ${palette.divider}`,
        borderRadius: 8,
        fontSize: 13,
      },
      labelStyle: { color: palette.text.primary, fontWeight: 600 },
      itemStyle: { color: palette.text.primary },
    },
  }
}

interface BarsProps {
  data: Bucket[]
  unit: string
  horizontal?: boolean
  height?: number
}

/* columns over time; horizontal bars when the labels are exercise names */
export function Bars({ data, unit, horizontal = false, height = 180 }: BarsProps) {
  const chart = useChartTheme()
  const tooltip = (
    <Tooltip {...chart.tooltip} cursor={chart.cursor}
             formatter={(value) => [`${Number(value).toLocaleString()} ${unit}`, null]} />
  )

  if (horizontal) {
    return (
      <BarChart data={data} layout="vertical" responsive style={{ width: '100%', height: data.length * 36 + 8 }}
                margin={{ top: 0, right: 16, bottom: 0, left: 0 }}>
        <XAxis type="number" hide />
        <YAxis type="category" dataKey="label" width={120} tick={chart.tick} tickLine={false} axisLine={false} />
        {tooltip}
        <Bar dataKey="value" fill={chart.bar} radius={[0, 4, 4, 0]} maxBarSize={24} />
      </BarChart>
    )
  }

  return (
    <BarChart data={data} responsive style={{ width: '100%', height }}
              margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
      <CartesianGrid vertical={false} stroke={chart.grid} />
      <XAxis dataKey="label" tick={chart.tick} tickLine={false} axisLine={{ stroke: chart.grid }}
             interval="preserveStartEnd" minTickGap={8} />
      <YAxis width={44} tick={chart.tick} tickLine={false} axisLine={false} allowDecimals={false}
             tickFormatter={compact} tickCount={4} />
      {tooltip}
      <Bar dataKey="value" fill={chart.bar} radius={[4, 4, 0, 0]} maxBarSize={24} />
    </BarChart>
  )
}
