import {Component, EventEmitter, Output, signal, effect, Injector, inject, OnInit} from '@angular/core';
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
  @Output() newDraft = new EventEmitter<void>();


  query = signal<string>('');

  all = signal<SessionsPage['items']>([]);
  page = signal<Pick<SessionsPage, 'items'>>({ items: [] });

  private injector = inject(Injector);

  constructor(private readonly sessions: SessionsService) {
    effect(() => {
      const q = this.normalize(this.query().trim());
      const base = this.all().filter(it => (it.preview ?? '').trim().length > 0);

      const filtered = q
        ? base.filter(it => this.normalize(it.title || 'New chat').startsWith(q))
        : base;

      this.page.set({ items: filtered });
    }, { injector: this.injector });
  }

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.sessions.list('', 0, 100).subscribe({
      next: pg => this.all.set(pg.items),
    });
  }

  onCreate(): void {
    this.newDraft.emit();
  }

  onSearchEnter(): void {
    this.refresh();
  }

  private normalize(s: string): string {
    return s
      .normalize('NFD')
      .replace(/\p{Diacritic}/gu, '')
      .toLowerCase();
  }
}
