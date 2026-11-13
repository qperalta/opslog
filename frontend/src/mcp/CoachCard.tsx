import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { Box, Card, CardContent, Divider, Stack, Typography } from '@mui/material'
import { timeAgo } from '../components/format'
import type { AiActivity, AiCoach } from './types'

// what each tool did
const DID: Record<string, string> = {
  get_checkin: 'Read your check-in',
  list_logs: 'Read your logged sets',
  get_plan: 'Read your plan',
  search_exercises: 'Searched the exercises',
  get_training_report: 'Read your training report',
  set_plan_day: 'Changed a plan day',
  write_session: 'Wrote a session',
  set_coach_summary: 'Wrote the summary'
}

// null when the call went through
const problemOf = (item: AiActivity) => {
  if(item.outcome === 'DENIED') return 'Refused: this token can only read'
  if(item.outcome === 'FAILED') return item.detail ?? 'Failed'
  return null
}

interface Props {
  coach: AiCoach
  programSummary: string
}

export function CoachCard({ coach, programSummary }: Props) {
  const latest = coach.recent[0]
  const problem = latest ? problemOf(latest) : null
  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Stack spacing={1.5}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <AutoAwesomeIcon color="primary" />
            <Box>
              <Typography sx={{ fontWeight: 600 }}>{coach.clientName ?? 'Your AI coach'} is coaching your plan</Typography>
              <Typography variant="caption" color="text.secondary">
                Last active {coach.lastActiveAt ? timeAgo(coach.lastActiveAt) : 'recently'} · {programSummary}
              </Typography>
            </Box>
          </Stack>
          <Divider />
          {/* fixed height so the week planner stays in the same place; a long summary scrolls inside the box */}
          <Box sx={{ height: 120, overflowY: 'auto', pr: 0.5 }}>
            {coach.summary ? (
              <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>{coach.summary}</Typography>
            ) : (
              <Typography variant="body2" color="text.secondary">No summary yet. Ask your assistant to plan your week.</Typography>
            )}
          </Box>
          {(coach.summaryAt || latest) && (
            <Typography variant="caption" color={problem ? 'error' : 'text.secondary'}>
              {[
                coach.summaryAt && `Updated ${timeAgo(coach.summaryAt)}`,
                latest && `Last: ${problem ?? DID[latest.tool] ?? latest.tool}, ${timeAgo(latest.at)}`
              ].filter(Boolean).join(' · ')}
            </Typography>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}