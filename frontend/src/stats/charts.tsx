import { Box, Card, CardContent, Stack, Typography, useTheme } from '@mui/material'
import type { ReactNode } from 'react'
import { Bar, BarChart, CartesianGrid, Pie, PieChart, Tooltip, XAxis, YAxis } from 'recharts'
import { compact } from './periods'
import type { Bucket } from './periods'

// push, pull, then the sets with neither; checked with the dataviz palette validator against the paper color
const SPLIT_COLORS = {
  light: ['#2a78d6', '#eb6834', '#8a9194'],
  dark: ['#3987e5', '#d95926', '#6b7477'],
}

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
    split: SPLIT_COLORS[palette.mode],
    surface: palette.background.paper,
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

/* the legend under the ring names each slice with its sets and share */
export function SplitDonut({ slices, total }: { slices: { name: string; value: number }[]; total: number }) {
  const chart = useChartTheme()
  const data = slices.map((slice, i) => ({ ...slice, fill: chart.split[Math.min(i, chart.split.length - 1)] }))

  return (
    <>
      <Box sx={{ position: 'relative' }}>
        <PieChart responsive style={{ width: '100%', height: 140 }}>
          <Pie data={data} dataKey="value" nameKey="name" innerRadius="62%" outerRadius="95%"
               stroke={chart.surface} strokeWidth={2} isAnimationActive={false} />
          <Tooltip {...chart.tooltip} formatter={(value, name) => [`${Number(value).toLocaleString()} sets`, name]} />
        </PieChart>
        <Box sx={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', pointerEvents: 'none' }}>
          <Typography sx={{ fontWeight: 700 }}>{compact(total)}</Typography>
        </Box>
      </Box>
      <Stack spacing={0.5} sx={{ mt: 1 }}>
        {data.map((slice) => (
          <Stack key={slice.name} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: slice.fill, flexShrink: 0 }} />
            <Typography variant="caption" sx={{ flexGrow: 1 }}>{slice.name}</Typography>
            <Typography variant="caption" color="text.secondary">
              {slice.value} · {Math.round((slice.value / total) * 100)}%
            </Typography>
          </Stack>
        ))}
      </Stack>
    </>
  )
}
