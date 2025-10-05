import { Component, EventEmitter, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SessionsService } from '../../../core/services/sessions/sessions';
import { SessionsPage } from '../../../core/models/sessions-page.model';
import { SessionCard } from '../../../shared/components/session-card/session-card';
import { UiButton } from '../../../shared/components/ui-button/ui-button';
import { TextInput } from '../../../shared/components/text-input/text-input';

@Component({
  selector: 'app-sessions-sidebar',
  standalone: true,
  imports: [CommonModule, SessionCard, UiButton, TextInput],
  templateUrl: './sessions-sidebar.html',
  styleUrl: './sessions-sidebar.scss'
})
export class SessionsSidebar {
  @Output() selectSession = new EventEmitter<number>();
  @Output() newSession = new EventEmitter<number>();

  query = signal('');
  page = signal<SessionsPage | null>(null);
  loading = signal(false);

  constructor(private readonly sessions: SessionsService) {
    this.refresh();
  }

  refresh() {
    this.loading.set(true);
    this.sessions.list(this.query(), 0, 30).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onCreate() {
    this.sessions.create('New chat').subscribe(res => {
      type CreateRes = { sessionId: number; title?: string | null };
      const { sessionId } = res as CreateRes;
      if (typeof sessionId === 'number') {
        this.newSession.emit(sessionId);
      }
      this.refresh();
    });
  }


  onSearchEnter() { this.refresh(); }
}
