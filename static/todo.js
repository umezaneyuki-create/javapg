(() => {
  let selectedTaskRow = null;

  function selectTask(row) {
    if (!row || row.classList.contains('context-only')) return;
    document.querySelectorAll('.selectable-row').forEach((item) => {
      item.classList.remove('selected-task-row');
    });
    selectedTaskRow = row;
    row.classList.add('selected-task-row');
    document.getElementById('selectedTaskId').value = row.dataset.id;
    document.getElementById('selectedParentId').value = row.dataset.parent === 'true' ? row.dataset.id : '';
    const selectedLabel = document.getElementById('selectedTask');
    selectedLabel.replaceChildren();
    const paw = document.createElement('img');
    paw.src = '/static/assets/paw.png';
    paw.alt = '';
    const label = document.createElement('span');
    label.textContent = `選択中：${row.dataset.title}`;
    selectedLabel.append(paw, label);
    document.getElementById('subtaskButton').disabled = row.dataset.parent !== 'true';
  }

  function openEditDialog() {
    if (!selectedTaskRow) {
      window.alert('編集するタスクを選択してください');
      return;
    }
    document.getElementById('editId').value = selectedTaskRow.dataset.id;
    document.getElementById('editTitle').value = selectedTaskRow.dataset.title;
    document.getElementById('editStart').value = selectedTaskRow.dataset.start;
    document.getElementById('editDeadline').value = selectedTaskRow.dataset.deadline;
    document.getElementById('editDialog').classList.remove('hidden');
  }

  function openDeleteDialog() {
    if (!selectedTaskRow) {
      window.alert('削除するタスクを選択してください');
      return;
    }
    document.getElementById('deleteTaskId').value = selectedTaskRow.dataset.id;
    document.getElementById('deleteMessage').textContent = selectedTaskRow.dataset.parent === 'true'
      ? 'このタスクとサブタスクを削除します。よろしいですか？'
      : 'このタスクを削除します。よろしいですか？';
    document.getElementById('deleteConfirm').classList.remove('hidden');
  }

  document.addEventListener('click', (event) => {
    const completion = event.target.closest('.completion-toggle');
    if (completion) {
      event.stopPropagation();
      return;
    }
    const row = event.target.closest('.selectable-row');
    if (row) selectTask(row);
  });

  document.getElementById('searchButton').addEventListener('click', () => {
    const form = document.querySelector('.todo-toolbar');
    const params = new URLSearchParams({
      filter: new URLSearchParams(window.location.search).get('filter') || 'all',
      sort: new URLSearchParams(window.location.search).get('sort') || 'desc',
      titleSearch: form.elements.todo.value,
      deadlineSearch: form.elements.deadline.value
    });
    window.location.href = `/?${params.toString()}`;
  });
  document.getElementById('editButton').addEventListener('click', openEditDialog);
  document.getElementById('deleteButton').addEventListener('click', openDeleteDialog);
  document.querySelectorAll('[data-close]').forEach((button) => {
    button.addEventListener('click', () => document.getElementById(button.dataset.close).classList.add('hidden'));
  });
})();
