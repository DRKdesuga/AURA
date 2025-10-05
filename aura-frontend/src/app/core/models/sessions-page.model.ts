import { SessionSummary } from './session-summary.model';

export interface SessionsPage {
  items: SessionSummary[];
  total: number;
  page: number;
  size: number;
}
