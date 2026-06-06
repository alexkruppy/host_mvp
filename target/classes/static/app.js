const API = '/api/persons';

const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');
const clearBtn = document.getElementById('clearBtn');
const personForm = document.getElementById('personForm');
const personId = document.getElementById('personId');
const firstName = document.getElementById('firstName');
const lastName = document.getElementById('lastName');
const email = document.getElementById('email');
const phoneNumber = document.getElementById('phoneNumber');
const saveBtn = document.getElementById('saveBtn');
const cancelBtn = document.getElementById('cancelBtn');
const resultsBody = document.getElementById('resultsBody');
const resultCount = document.getElementById('resultCount');
const emptyMessage = document.getElementById('emptyMessage');

async function request(method, url, body) {
    const options = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) options.body = JSON.stringify(body);
    const res = await fetch(url, options);
    if (!res.ok) throw new Error(await res.text());
    return res.status === 204 ? null : res.json();
}

function renderPersons(persons) {
    resultsBody.innerHTML = '';
    if (!persons || persons.length === 0) {
        emptyMessage.style.display = 'block';
        resultCount.textContent = '0';
        return;
    }
    emptyMessage.style.display = 'none';
    resultCount.textContent = persons.length;

    persons.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${p.id}</td>
            <td>${escapeHtml(p.firstName)}</td>
            <td>${escapeHtml(p.lastName)}</td>
            <td>${escapeHtml(p.email || '')}</td>
            <td>${escapeHtml(p.phoneNumber || '')}</td>
            <td>
                <button class="edit-btn" onclick="editPerson(${p.id})">✎</button>
                <button class="delete-btn" onclick="deletePerson(${p.id})">✕</button>
            </td>
        `;
        resultsBody.appendChild(tr);
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadPersons(query) {
    const url = query ? `${API}/search?q=${encodeURIComponent(query)}` : API;
    const data = await request('GET', url);
    renderPersons(data);
}

searchBtn.addEventListener('click', () => {
    const q = searchInput.value.trim();
    loadPersons(q).catch(err => alert('Ошибка поиска: ' + err.message));
});

searchInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') searchBtn.click();
});

clearBtn.addEventListener('click', () => {
    searchInput.value = '';
    loadPersons('').catch(err => alert('Ошибка загрузки: ' + err.message));
});

personForm.addEventListener('submit', async e => {
    e.preventDefault();
    const body = {
        firstName: firstName.value.trim(),
        lastName: lastName.value.trim(),
        email: email.value.trim() || null,
        phoneNumber: phoneNumber.value.trim() || null,
    };
    const id = personId.value;
    try {
        if (id) {
            await request('PUT', `${API}/${id}`, body);
        } else {
            await request('POST', API, body);
        }
        resetForm();
        const q = searchInput.value.trim();
        await loadPersons(q);
    } catch (err) {
        alert('Ошибка сохранения: ' + err.message);
    }
});

cancelBtn.addEventListener('click', resetForm);

async function editPerson(id) {
    try {
        const person = await request('GET', `${API}/${id}`);
        personId.value = person.id;
        firstName.value = person.firstName;
        lastName.value = person.lastName;
        email.value = person.email || '';
        phoneNumber.value = person.phoneNumber || '';
        saveBtn.textContent = 'Обновить';
        cancelBtn.style.display = 'inline-block';
        firstName.focus();
    } catch (err) {
        alert('Ошибка загрузки: ' + err.message);
    }
}

async function deletePerson(id) {
    if (!confirm('Удалить этого человека?')) return;
    try {
        await request('DELETE', `${API}/${id}`);
        const q = searchInput.value.trim();
        await loadPersons(q);
    } catch (err) {
        alert('Ошибка удаления: ' + err.message);
    }
}

function resetForm() {
    personForm.reset();
    personId.value = '';
    saveBtn.textContent = 'Сохранить';
    cancelBtn.style.display = 'none';
}

loadPersons('').catch(err => alert('Ошибка загрузки: ' + err.message));
