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

  /** Adds a floating "Copy" button to each <pre> block (top-right). */
  private attachCopyButtons() {
    const root = this.host.nativeElement.querySelector('.md');
    if (!root) return;

    const blocks = root.querySelectorAll('pre');
    blocks.forEach((pre) => {
      const preEl = pre as HTMLElement;
      preEl.style.position = 'relative';
      if (preEl.querySelector('.copy-btn')) return;

      const btn = document.createElement('button');
      btn.className = 'copy-btn';
      btn.type = 'button';
      btn.textContent = 'Copy';

      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const code = preEl.querySelector('code')?.textContent ?? '';
        try {
          await navigator.clipboard.writeText(code);
          const old = btn.textContent;
          btn.textContent = 'Copied';
          setTimeout(() => (btn.textContent = old || 'Copy'), 1200);
        } catch {
          const old = btn.textContent;
          btn.textContent = 'Failed';
          setTimeout(() => (btn.textContent = old || 'Copy'), 1200);
        }
      });

      preEl.appendChild(btn);
    });
  }
}
