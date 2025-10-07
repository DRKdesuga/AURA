import {Component, EventEmitter, OnInit, Output, signal} from '@angular/core';
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
export class SessionsSidebar implements OnInit{
  @Output() selectSession = new EventEmitter<number>();
  @Output() newDraft = new EventEmitter<void>(); // no DB creation here

  query = signal('');
  page = signal<{ items: SessionsPage['items'] }>({ items: [] });

  constructor(private readonly sessions: SessionsService) {}

  ngOnInit() { this.refresh(); }

  refresh() {
    this.sessions.list(this.query(), 0, 50).subscribe({
      next: (pg) => {
        const filtered = pg.items.filter(it => (it.preview ?? '').trim().length > 0);
        this.page.set({ items: filtered });
      }
    });
  }

  onCreate() {
    this.newDraft.emit();
  }

  onSearch(val: string) {
    this.query.set(val);
    this.refresh();
  }

  onSearchEnter() {
    this.refresh();
  }
}
