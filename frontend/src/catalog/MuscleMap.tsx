import { useTheme } from '@mui/material'
import regions from './bodyRegions.json'

// body outline, drawn once per view
const BODY = 'M88 95 L88 83 Q74 75 76 55 Q76 34 100 34 Q124 34 124 55 Q126 75 112 83 L112 95 L130 105 Q145 108 149 131 L157 178 L166 230 L164 253 Q161 266 155 265 L150 252 L146 259 L143 255 L145 234 L135 198 L128 170 L127 193 L139 233 Q147 273 137 314 L136 331 L139 369 L132 416 L140 431 L138 438 L113 438 L109 429 L115 411 L111 371 L112 337 L105 303 L100 271 L95 303 L88 337 L89 371 L85 411 L91 429 L87 438 L62 438 L60 431 L68 416 L61 369 L64 331 L63 314 Q53 273 61 233 L73 193 L72 170 L65 198 L55 234 L57 255 L54 259 L50 252 L45 265 Q39 266 36 253 L34 230 L43 178 L51 131 Q55 108 70 105 Z'

const VIEWS = [25, 265]

/* muscle group name (from the database) -> region keys in bodyRegions.json */
const TARGETS: Record<string, string[]> = {
  Quadriceps: ['quads'],
  Hamstrings: ['hamstrings'],
  Glutes: ['glutes'],
  Calves: ['calves'],
  'Lower Back': ['lower-back'],
  Chest: ['chest'],
  Shoulders: ['front-delts', 'rear-delts'],
  Traps: ['upper-traps', 'lower-traps'],
  'Upper Back': ['rhomboids', 'lower-traps', 'rear-delts'],
  Lats: ['lats'],
  Biceps: ['biceps'],
  Triceps: ['triceps'],
  Forearms: ['forearms'],
  Core: ['abs', 'obliques'],
  'Full Body': ['quads', 'hamstrings', 'glutes', 'chest', 'lats', 'upper-traps', 'rhomboids', 'abs',
    'front-delts', 'rear-delts', 'lower-back', 'calves'],
}

interface Props {
  muscle: string
  width?: number
  labels?: boolean
}

/* front and back figure with the exercise's muscle group highlighted; an unknown group shows no highlight */
export function MuscleMap({ muscle, width = 110, labels = false }: Props) {
  const theme = useTheme()
  const targets = TARGETS[muscle] ?? []

  return (
    <svg
      viewBox={labels ? '0 0 490 470' : '50 28 390 416'}
      width={width}
      role="img"
      aria-label={`Targets ${muscle}`}
      style={{ flexShrink: 0, display: 'block' }}
    >
      {VIEWS.map((offset) => (
        <path key={offset} d={BODY} transform={`translate(${offset},0)`} fill="#F0F3F3" stroke="#9CAEB1" strokeWidth={1.5} />
      ))}
      {regions.map((region) => (
        <path
          key={region.id}
          d={region.d}
          // a mirrored region reuses the other side's path, flipped around the body's center line
          transform={`translate(${region.offset},0)${region.mirror ? ' translate(200,0) scale(-1,1)' : ''}`}
          fill={targets.includes(region.muscle) ? theme.palette.secondary.main : '#C6D4D6'}
          stroke="white"
          strokeWidth={1.7}
          strokeLinejoin="round"
        />
      ))}
      {labels && (
        <g fill={theme.palette.primary.main} fontSize={11} fontWeight={700} textAnchor="middle" letterSpacing={2}>
          <text x={125} y={461}>FRONT</text>
          <text x={365} y={461}>BACK</text>
        </g>
      )}
    </svg>
  )
}
