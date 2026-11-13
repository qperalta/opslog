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
  write_session: 'Wrote a session'
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
          <Typography variant="overline" color="text.secondary" sx={{ lineHeight: 1 }}>Recent AI activity</Typography>
          {coach.recent.length === 0 ? (
            <Typography variant="body2" color="text.secondary">Nothing yet. Ask your assistant to look at your plan.</Typography>
          ) : coach.recent.map((item, index) => {
            const problem = problemOf(item)
            return (
              <Box key={`${item.at}-${index}`}>
                <Typography variant="body2">{DID[item.tool] ?? item.tool}</Typography>
                <Typography variant="caption" color={problem ? 'error' : 'text.secondary'}>
                  {problem ? `${problem} · ` : ''}{timeAgo(item.at)}
                </Typography>
              </Box>
            )
            })}
        </Stack>
      </CardContent>
    </Card>
  )
}