import { Component, Input, OnChanges, SimpleChanges, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { marked } from 'marked';
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js';
import DOMPurify from 'dompurify';

// Configure marked once (v9+)
let markedConfigured = false;
function setupMarked() {
  if (markedConfigured) return;
  marked.setOptions({ gfm: true, breaks: true });
  marked.use(
    markedHighlight({
      langPrefix: 'hljs language-',
      highlight(code: string, lang?: string): string {
        try {
          if (lang && hljs.getLanguage(lang)) {
            return hljs.highlight(code, { language: lang }).value;
          }
          return hljs.highlightAuto(code).value;
        } catch {
          return code;
        }
      }
    })
  );
  markedConfigured = true;
}

@Component({
  selector: 'app-markdown-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './markdown-viewer.html',
  styleUrl: './markdown-viewer.scss'
})
export class MarkdownViewer implements OnChanges {
  @Input() content = '';
  html = '';

  constructor(private host: ElementRef<HTMLElement>) {
    setupMarked();
  }

  ngOnChanges(_: SimpleChanges): void {
    const parsed = marked.parse(this.content ?? '');
    const safe = DOMPurify.sanitize(String(parsed));
    this.html = safe;
    queueMicrotask(() => this.attachCopyButtons());
  }

  private attachCopyButtons() {
    const root = this.host.nativeElement.querySelector('.md');
    if (!root) return;

    const copyIcon = () => `
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path fill="currentColor"
        d="M16 1H6a2 2 0 0 0-2 2v12h2V3h10V1zm3 4H10a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H10V7h9v14z"/>
    </svg>`;
    const checkIcon = () => `
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path fill="currentColor"
        d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z"/>
    </svg>`;

    const blocks = root.querySelectorAll('pre');
    blocks.forEach((pre) => {
      const preEl = pre as HTMLElement;
      preEl.style.position = 'relative';
      if (preEl.querySelector('.copy-btn')) return;

      const btn = document.createElement('button');
      btn.className = 'copy-btn';
      btn.type = 'button';
      btn.setAttribute('aria-label', 'Copy code');
      btn.setAttribute('title', 'Copy code');
      btn.innerHTML = copyIcon();                

      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const code = preEl.querySelector('code')?.textContent ?? '';
        try {
          await navigator.clipboard.writeText(code);
          btn.classList.add('ok');
          const old = btn.innerHTML;
          btn.innerHTML = checkIcon();
          setTimeout(() => {
            btn.classList.remove('ok');
            btn.innerHTML = old;
          }, 1200);
        } catch {
        }
      });

      preEl.appendChild(btn);
    });
  }

}
