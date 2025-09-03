const API = window.location.origin + '/api';
let boardColors = [];
let attempts = Array.from({ length: 6 }, () => Array(5).fill(0));
let mode = 'standard';
let clueUsed = false;
let phantomUsed = false;

window.addEventListener('DOMContentLoaded', async () => {
  document.querySelectorAll('input[name=mode]')
    .forEach(r => r.addEventListener('change', e => mode = e.target.value));

  await fetch(API + '/new');
  boardColors = await (await fetch(API + '/board')).json();

  renderBoard();
  attachCellHandlers();
  setupTools();
});

function renderBoard() {
  const bd = document.getElementById('board');
  bd.innerHTML = '';
  boardColors.forEach((row, r) => {
    row.forEach((col, c) => {
      const cell = document.createElement('div');
      cell.className = `cell ${col}`;
      cell.dataset.row = r;
      cell.dataset.col = c;
      bd.append(cell);
    });
  });
}

function attachCellHandlers() {
  document.querySelectorAll('#board .cell').forEach(cell => {
    cell.addEventListener('click', () => {
      if (cell.classList.contains('revealed') || cell.querySelector('input'))
        return;
      openCellInput(cell);
    });
  });
}

function openCellInput(cell) {
  const r = +cell.dataset.row, c = +cell.dataset.col;
  const input = document.createElement('input');
  input.className = 'cell-input-overlay';
  input.maxLength = 1;
  cell.append(input);
  input.focus();

  // remove overlay on blur
  input.addEventListener('blur', () => {
    if (cell.contains(input)) cell.removeChild(input);
  });

  input.addEventListener('keydown', async e => {
    if (e.key === 'Enter') {
      const letter = input.value.trim().toLowerCase();
      if (!/^[a-z]$/.test(letter)) return;
      await submitCell(r, c, letter, cell);
      if (cell.contains(input)) cell.removeChild(input);
    }
  });
}

async function submitCell(r, c, letter, cell) {
  const maxTries = mode === 'forgiving' ? 5 : 3;
  if (attempts[r][c] >= maxTries) return;

  attempts[r][c]++;
  const res = await fetch(API + '/validate-letter', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ row: r, col: c, letter })
  });
  const { correct } = await res.json();

  if (correct) {
    cell.textContent = letter.toUpperCase();
    cell.classList.add('revealed');
    const counter = cell.querySelector('.attempts-counter');
    if (counter) counter.remove();
  } else {
    triggerScreenGlitch();
    updateCounter(cell, maxTries - attempts[r][c]);
  }
}

function updateCounter(cell, remaining) {
  let counter = cell.querySelector('.attempts-counter');
  if (!counter) {
    counter = document.createElement('span');
    counter.className = 'attempts-counter';
    cell.append(counter);
  }
  counter.textContent = remaining;
}

function triggerScreenGlitch() {
  document.body.classList.add('glitch-screen');
  setTimeout(() => document.body.classList.remove('glitch-screen'), 500);
}

function setupTools() {
  // Letter counter
  document.getElementById('f1').addEventListener('input', async e => {
    const l = e.target.value.trim().toLowerCase();
    const o = document.getElementById('o1');
    if (/^[a-z]$/.test(l)) {
      const r = await fetch(`${API}/count?letter=${l}`);
      o.textContent = (await r.json()).count;
    } else o.textContent = '–';
  });

  // One-time clue
  document.getElementById('revealClue').onclick = async () => {
    if (clueUsed) return;
    const idx = Math.floor(Math.random() * 6);
    const res = await fetch(`${API}/reveal?idx=${idx}`);
    if (!res.ok) return;
    const { word, colors } = await res.json();
    const cells = Array.from(document.querySelectorAll('#board .cell'))
      .slice(idx*5, idx*5 + 5);
    cells.forEach((c,i) => {
      c.className = `cell ${colors[i]} revealed`;
      c.textContent = word[i].toUpperCase();
    });
    clueUsed = true;
  };

  // Scratch box
  document.getElementById('scratchBtn').onclick = async () => {
    const val = document.getElementById('scratchIn').value.trim().toLowerCase();
    if (val.length !== 5) return;
    const res = await fetch(API + '/scratch', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ guess: val })
    });
    if (!res.ok) return;
    const { colors } = await res.json();
    const row = document.createElement('div');
    row.className = 'row';
    val.toUpperCase().split('').forEach((ch,i) => {
      const c = document.createElement('div');
      c.className = `cell ${colors[i]}`;
      c.textContent = ch;
      row.append(c);
    });
    const container = document.getElementById('scratchRows');
    container.append(row);
    if (container.children.length > 7) container.removeChild(container.firstChild);
  };

  // Phantom Scan
  document.getElementById('phantomScanBtn').onclick = async () => {
    if (phantomUsed) return;
    phantomUsed = true;
    const res = await fetch(`${API}/phantom-scan`);
    const data = await res.json();
    const grid = document.getElementById('phantomGrid');
    grid.innerHTML = '';
    let i = 0;
    for (const [letter, used] of Object.entries(data)) {
      const div = document.createElement('div');
      div.className = `letter-cell ${used ? 'letter-used' : 'letter-unused'}`;
      div.textContent = letter;
      grid.append(div);
      // staggered pop-in
      setTimeout(() => div.classList.add('show'), 50 * (i++));
    }
    document.getElementById('phantomScanBtn').disabled = true;
  };
}
