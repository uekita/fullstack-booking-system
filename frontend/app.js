const API = '/api';

const state = { couriers: [], meta: { zones: [], vehicleTypes: [] } };

// ---------- helpers ----------
async function api(path, options = {}) {
    const res = await fetch(API + path, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (res.status === 204) return null;
    const body = await res.json().catch(() => null);
    if (!res.ok) {
        let msg = (body && body.message) || `Request failed (${res.status})`;
        if (body && body.fieldErrors) {
            msg += ': ' + Object.entries(body.fieldErrors).map(([f, m]) => `${f} ${m}`).join(', ');
        }
        throw new Error(msg);
    }
    return body;
}

let notifyTimer;
function notify(message, type = 'success') {
    const el = document.getElementById('notification');
    el.textContent = message;
    el.className = `notification ${type}`;
    clearTimeout(notifyTimer);
    notifyTimer = setTimeout(() => el.classList.add('hidden'), 4000);
}

const fmt = (iso) => iso ? new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '';

// Converts an ISO instant to a value usable by <input type="datetime-local"> (local time)
function toLocalInput(iso) {
    const d = new Date(iso);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const badge = (text) => `<span class="badge ${text}">${text.replace('_', ' ')}</span>`;

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fillSelect(select, items, { placeholder, value = (x) => x, label = (x) => x } = {}) {
    const keepFirst = placeholder !== undefined;
    select.innerHTML = keepFirst ? `<option value="">${placeholder}</option>` : '';
    items.forEach((it) => {
        const opt = document.createElement('option');
        opt.value = value(it);
        opt.textContent = label(it);
        select.appendChild(opt);
    });
}

function toggleEmpty(id, isEmpty) {
    document.getElementById(id).classList.toggle('hidden', !isEmpty);
}

// ---------- tabs ----------
document.querySelectorAll('nav .tab').forEach((btn) => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('nav .tab').forEach((b) => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.section').forEach((s) => s.classList.add('hidden'));
        document.getElementById(`${btn.dataset.tab}-section`).classList.remove('hidden');
        loaders[btn.dataset.tab]();
    });
});

document.querySelectorAll('dialog [data-close]').forEach((b) =>
    b.addEventListener('click', () => b.closest('dialog').close()));

// ---------- shifts ----------
async function loadShifts() {
    const params = new URLSearchParams();
    const zone = document.getElementById('shift-zone-filter').value;
    if (zone) params.set('zone', zone);
    if (document.getElementById('shift-upcoming-filter').checked) params.set('from', new Date().toISOString());
    try {
        const shifts = await api(`/shifts?${params}`);
        const tbody = document.querySelector('#shifts-table tbody');
        tbody.innerHTML = shifts.map((s) => {
            const editable = s.status === 'OPEN' || s.status === 'FULL';
            return `
            <tr data-id="${s.id}">
                <td>${s.id}</td>
                <td>${s.zone}</td>
                <td>${fmt(s.startTime)}</td>
                <td>${fmt(s.endTime)}</td>
                <td>${s.bookedCount} / ${s.capacity}</td>
                <td>${badge(s.status)}</td>
                <td class="actions">
                    <button class="primary" data-action="book" ${s.status === 'OPEN' ? '' : 'disabled'}>Book</button>
                    <button data-action="edit" ${editable ? '' : 'disabled'}>Edit</button>
                    <button class="danger" data-action="delete">Delete</button>
                </td>
            </tr>`;
        }).join('');
        toggleEmpty('shifts-empty', shifts.length === 0);
        tbody.querySelectorAll('tr').forEach((tr) => {
            const shift = shifts.find((s) => s.id === Number(tr.dataset.id));
            tr.querySelector('[data-action="book"]').onclick = () => openBookDialog(shift);
            tr.querySelector('[data-action="edit"]').onclick = () => openShiftDialog(shift);
            tr.querySelector('[data-action="delete"]').onclick = () => deleteShift(shift);
        });
    } catch (e) {
        notify(e.message, 'error');
    }
}

function openShiftDialog(shift) {
    document.getElementById('shift-form-title').textContent = shift ? `Edit shift #${shift.id}` : 'New shift';
    document.getElementById('shift-id').value = shift ? shift.id : '';
    document.getElementById('shift-zone').value = shift ? shift.zone : state.meta.zones[0];
    document.getElementById('shift-start').value = shift ? toLocalInput(shift.startTime) : '';
    document.getElementById('shift-end').value = shift ? toLocalInput(shift.endTime) : '';
    document.getElementById('shift-capacity').value = shift ? shift.capacity : 1;
    document.getElementById('shift-dialog').showModal();
}

document.getElementById('shift-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('shift-id').value;
    const payload = {
        zone: document.getElementById('shift-zone').value,
        startTime: new Date(document.getElementById('shift-start').value).toISOString(),
        endTime: new Date(document.getElementById('shift-end').value).toISOString(),
        capacity: Number(document.getElementById('shift-capacity').value),
    };
    try {
        await api(id ? `/shifts/${id}` : '/shifts', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        document.getElementById('shift-dialog').close();
        notify(id ? 'Shift updated' : 'Shift created');
        loadShifts();
    } catch (err) {
        notify(err.message, 'error');
    }
});

async function deleteShift(shift) {
    if (!confirm(`Delete shift #${shift.id} (${shift.zone})?`)) return;
    try {
        await api(`/shifts/${shift.id}`, { method: 'DELETE' });
        notify('Shift deleted');
        loadShifts();
    } catch (e) {
        notify(e.message, 'error');
    }
}

async function openBookDialog(shift) {
    try {
        const [couriers, bookings] = await Promise.all([
            api('/couriers?active=true'),
            api(`/shifts/${shift.id}/bookings`),
        ]);
        document.getElementById('book-shift-id').value = shift.id;
        document.getElementById('book-shift-summary').textContent =
            `#${shift.id} · ${shift.zone} · ${fmt(shift.startTime)} → ${fmt(shift.endTime)} · ${shift.availableSlots} slot(s) left`;
        fillSelect(document.getElementById('book-courier'), couriers, {
            placeholder: 'Select a courier',
            value: (c) => c.id,
            label: (c) => `${c.name} (${c.vehicleType})`,
        });
        const confirmed = bookings.filter((b) => b.status === 'CONFIRMED');
        document.getElementById('book-shift-bookings').innerHTML = confirmed.length
            ? confirmed.map((b) => `<li>${escapeHtml(b.courierName)}</li>`).join('')
            : '<li><em>None yet</em></li>';
        document.getElementById('book-dialog').showModal();
    } catch (e) {
        notify(e.message, 'error');
    }
}

document.getElementById('book-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        shiftId: Number(document.getElementById('book-shift-id').value),
        courierId: Number(document.getElementById('book-courier').value),
    };
    try {
        await api('/bookings', { method: 'POST', body: JSON.stringify(payload) });
        document.getElementById('book-dialog').close();
        notify('Shift booked');
        loadShifts();
    } catch (err) {
        notify(err.message, 'error');
    }
});

document.getElementById('new-shift-btn').addEventListener('click', () => openShiftDialog(null));
document.getElementById('shift-zone-filter').addEventListener('change', loadShifts);
document.getElementById('shift-upcoming-filter').addEventListener('change', loadShifts);

// ---------- couriers ----------
async function loadCouriers() {
    try {
        const couriers = await api('/couriers');
        state.couriers = couriers;
        const tbody = document.querySelector('#couriers-table tbody');
        tbody.innerHTML = couriers.map((c) => `
            <tr data-id="${c.id}">
                <td>${c.id}</td>
                <td>${escapeHtml(c.name)}</td>
                <td>${escapeHtml(c.email)}</td>
                <td>${escapeHtml(c.phone)}</td>
                <td>${c.vehicleType}</td>
                <td>${badge(c.active ? 'ACTIVE' : 'INACTIVE')}</td>
                <td class="actions">
                    <button data-action="edit">Edit</button>
                    <button data-action="bookings">Bookings</button>
                    <button class="danger" data-action="delete">Delete</button>
                </td>
            </tr>`).join('');
        toggleEmpty('couriers-empty', couriers.length === 0);
        tbody.querySelectorAll('tr').forEach((tr) => {
            const courier = couriers.find((c) => c.id === Number(tr.dataset.id));
            tr.querySelector('[data-action="edit"]').onclick = () => openCourierDialog(courier);
            tr.querySelector('[data-action="delete"]').onclick = () => deleteCourier(courier);
            tr.querySelector('[data-action="bookings"]').onclick = () => {
                document.getElementById('booking-courier-filter').value = courier.id;
                document.getElementById('tab-bookings').click();
            };
        });
    } catch (e) {
        notify(e.message, 'error');
    }
}

function openCourierDialog(courier) {
    document.getElementById('courier-form-title').textContent = courier ? `Edit courier #${courier.id}` : 'New courier';
    document.getElementById('courier-id').value = courier ? courier.id : '';
    document.getElementById('courier-name').value = courier ? courier.name : '';
    document.getElementById('courier-email').value = courier ? courier.email : '';
    document.getElementById('courier-phone').value = courier ? courier.phone : '';
    document.getElementById('courier-vehicle').value = courier ? courier.vehicleType : state.meta.vehicleTypes[0];
    document.getElementById('courier-active').checked = courier ? courier.active : true;
    document.getElementById('courier-dialog').showModal();
}

document.getElementById('courier-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('courier-id').value;
    const payload = {
        name: document.getElementById('courier-name').value,
        email: document.getElementById('courier-email').value,
        phone: document.getElementById('courier-phone').value,
        vehicleType: document.getElementById('courier-vehicle').value,
        active: document.getElementById('courier-active').checked,
    };
    try {
        await api(id ? `/couriers/${id}` : '/couriers', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        document.getElementById('courier-dialog').close();
        notify(id ? 'Courier updated' : 'Courier created');
        loadCouriers();
    } catch (err) {
        notify(err.message, 'error');
    }
});

async function deleteCourier(courier) {
    if (!confirm(`Delete courier ${courier.name}?`)) return;
    try {
        await api(`/couriers/${courier.id}`, { method: 'DELETE' });
        notify('Courier deleted');
        loadCouriers();
    } catch (e) {
        notify(e.message, 'error');
    }
}

document.getElementById('new-courier-btn').addEventListener('click', () => openCourierDialog(null));

// ---------- bookings ----------
async function loadBookings() {
    const courierSelect = document.getElementById('booking-courier-filter');
    const selected = courierSelect.value;
    try {
        const couriers = await api('/couriers');
        fillSelect(courierSelect, couriers, { placeholder: 'All couriers', value: (c) => c.id, label: (c) => c.name });
        courierSelect.value = selected;

        const params = new URLSearchParams();
        if (selected) params.set('courierId', selected);
        const status = document.getElementById('booking-status-filter').value;
        if (status) params.set('status', status);

        const bookings = await api(`/bookings?${params}`);
        const now = new Date();
        const tbody = document.querySelector('#bookings-table tbody');
        tbody.innerHTML = bookings.map((b) => {
            const cancellable = b.status === 'CONFIRMED' && new Date(b.shiftStart) > now;
            const reschedulable = b.status === 'CONFIRMED' && new Date(b.shiftStart) > now;
            return `
            <tr data-id="${b.id}">
                <td>${b.id}</td>
                <td>${escapeHtml(b.courierName)}</td>
                <td>#${b.shiftId}</td>
                <td>${b.zone}</td>
                <td>${fmt(b.shiftStart)} → ${fmt(b.shiftEnd)}</td>
                <td>${badge(b.status)}</td>
                <td>${fmt(b.createdAt)}</td>
                <td class="actions">
                    <button data-action="reschedule" ${reschedulable ? '' : 'disabled'}>Reschedule</button>
                    <button class="danger" data-action="cancel" ${cancellable ? '' : 'disabled'}>Cancel</button>
                </td>
            </tr>`;
        }).join('');
        toggleEmpty('bookings-empty', bookings.length === 0);
        tbody.querySelectorAll('tr').forEach((tr) => {
            const booking = bookings.find((b) => b.id === Number(tr.dataset.id));
            tr.querySelector('[data-action="reschedule"]').onclick = () => openRescheduleDialog(booking);
            tr.querySelector('[data-action="cancel"]').onclick = () => cancelBooking(Number(tr.dataset.id));
        });
    } catch (e) {
        notify(e.message, 'error');
    }
}

async function cancelBooking(id) {
    if (!confirm(`Cancel booking #${id}?`)) return;
    try {
        await api(`/bookings/${id}/cancel`, { method: 'POST' });
        notify('Booking cancelled');
        loadBookings();
    } catch (e) {
        notify(e.message, 'error');
    }
}

// ---------- reschedule ----------
const rescheduleState = {
    booking: null,
    shifts: [],
    filtered: [],
    selectedId: null,
    activeIndex: -1,
};

let rescheduleFilterTimer;

function collapseShiftList() {
    document.getElementById('reschedule-shift-list').classList.add('hidden');
    document.getElementById('reschedule-shift-search').setAttribute('aria-expanded', 'false');
}

async function openRescheduleDialog(booking) {
    clearTimeout(rescheduleFilterTimer);
    rescheduleFilterTimer = null;
    rescheduleState.booking = booking;
    rescheduleState.shifts = [];
    rescheduleState.filtered = [];
    rescheduleState.selectedId = null;
    rescheduleState.activeIndex = -1;

    document.getElementById('reschedule-summary').textContent =
        `#${booking.id} · ${booking.courierName} · currently #${booking.shiftId} (${booking.zone})`;
    const input = document.getElementById('reschedule-shift-search');
    input.value = '';
    input.setAttribute('aria-expanded', 'false');
    input.removeAttribute('aria-activedescendant');
    document.getElementById('reschedule-confirm-btn').disabled = true;

    const list = document.getElementById('reschedule-shift-list');
    list.classList.remove('hidden');
    list.innerHTML = '<li class="combobox-status">Loading shifts…</li>';
    input.setAttribute('aria-expanded', 'true');
    document.getElementById('reschedule-dialog').showModal();

    try {
        const shifts = await api(`/shifts?from=${encodeURIComponent(new Date().toISOString())}`);
        if (rescheduleState.booking !== booking) return;
        rescheduleState.shifts = shifts.filter((s) => s.id !== booking.shiftId);
        // The user may have typed while the list was loading: apply the current search term instead of
        // overwriting it with the full list, otherwise the visible filter and the results disagree.
        applyShiftFilter();
    } catch (e) {
        list.innerHTML = '<li class="combobox-status">Could not load shifts</li>';
        notify(e.message, 'error');
    }
}

function renderShiftOptions() {
    const list = document.getElementById('reschedule-shift-list');
    const input = document.getElementById('reschedule-shift-search');
    const selectable = rescheduleState.filtered.filter((s) => s.availableSlots > 0);
    const activeShift = rescheduleState.activeIndex >= 0 ? selectable[rescheduleState.activeIndex] : null;

    if (!rescheduleState.filtered.length) {
        list.innerHTML = '<li class="combobox-status">No shifts match</li>';
        list.classList.remove('hidden');
        input.setAttribute('aria-expanded', 'true');
        input.removeAttribute('aria-activedescendant');
        return;
    }

    list.innerHTML = rescheduleState.filtered.map((s) => {
        const disabled = s.availableSlots <= 0;
        const active = activeShift !== null && activeShift.id === s.id;
        return `
            <li role="option"
                id="reschedule-option-${s.id}"
                data-shift-id="${s.id}"
                aria-selected="${rescheduleState.selectedId === s.id}"
                aria-disabled="${disabled}"
                class="combobox-option${disabled ? ' disabled' : ''}${active ? ' active' : ''}">
                #${s.id} · ${escapeHtml(s.zone)} · ${fmt(s.startTime)}–${fmt(s.endTime)} ·
                ${disabled ? 'Full' : `${s.availableSlots} slot(s)`}
            </li>`;
    }).join('');

    list.classList.remove('hidden');
    input.setAttribute('aria-expanded', 'true');
    if (activeShift !== null) {
        input.setAttribute('aria-activedescendant', `reschedule-option-${activeShift.id}`);
    } else {
        input.removeAttribute('aria-activedescendant');
    }
    list.querySelectorAll('[role="option"]').forEach((li) => {
        li.addEventListener('click', () => selectShiftOption(Number(li.dataset.shiftId)));
    });
}

function selectShiftOption(id) {
    const shift = rescheduleState.shifts.find((s) => s.id === id);
    if (!shift || shift.availableSlots <= 0) return;

    rescheduleState.selectedId = id;
    rescheduleState.activeIndex = -1;
    const input = document.getElementById('reschedule-shift-search');
    input.value = `#${shift.id} · ${shift.zone} · ${fmt(shift.startTime)}–${fmt(shift.endTime)}`;
    input.removeAttribute('aria-activedescendant');
    document.querySelectorAll('#reschedule-shift-list [role="option"]').forEach((el) => {
        el.setAttribute('aria-selected', String(Number(el.dataset.shiftId) === id));
        el.classList.remove('active');
    });
    collapseShiftList();
    document.getElementById('reschedule-confirm-btn').disabled = false;
}

function applyShiftFilter() {
    const term = document.getElementById('reschedule-shift-search').value.trim().toLowerCase();
    // The options are labelled "#<id>", so accept both "123" and "#123".
    const idTerm = term.startsWith('#') ? term.slice(1) : term;
    rescheduleState.filtered = rescheduleState.shifts.filter((s) =>
        String(s.id).includes(idTerm) || s.zone.toLowerCase().includes(term));
    rescheduleState.activeIndex = -1;
    renderShiftOptions();
}

document.getElementById('reschedule-shift-search').addEventListener('input', () => {
    clearTimeout(rescheduleFilterTimer);
    rescheduleState.selectedId = null;
    rescheduleState.activeIndex = -1;
    document.getElementById('reschedule-confirm-btn').disabled = true;

    rescheduleFilterTimer = setTimeout(() => {
        rescheduleFilterTimer = null;
        applyShiftFilter();
    }, 250);
});

document.getElementById('reschedule-shift-search').addEventListener('keydown', (e) => {
    // Apply a pending debounced filter immediately so a fast ArrowDown/Enter cannot select
    // an option from the previous, stale result list.
    if (rescheduleFilterTimer) {
        clearTimeout(rescheduleFilterTimer);
        rescheduleFilterTimer = null;
        applyShiftFilter();
    }

    const list = document.getElementById('reschedule-shift-list');
    const options = rescheduleState.filtered.filter((s) => s.availableSlots > 0);

    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        if (!options.length) return;
        e.preventDefault();
        const delta = e.key === 'ArrowDown' ? 1 : -1;
        if (rescheduleState.activeIndex < 0) {
            rescheduleState.activeIndex = delta > 0 ? 0 : options.length - 1;
        } else {
            rescheduleState.activeIndex =
                (rescheduleState.activeIndex + delta + options.length) % options.length;
        }
        renderShiftOptions();
        const active = options[rescheduleState.activeIndex];
        const el = list.querySelector(`[data-shift-id="${active.id}"]`);
        if (el) el.scrollIntoView({ block: 'nearest' });
    } else if (e.key === 'Enter') {
        if (rescheduleState.activeIndex < 0 || !options.length) return;
        e.preventDefault();
        selectShiftOption(options[rescheduleState.activeIndex].id);
    } else if (e.key === 'Escape' && !list.classList.contains('hidden')) {
        e.preventDefault();
        e.stopPropagation();
        collapseShiftList();
    }
});

function openShiftList() {
    document.getElementById('reschedule-shift-list').classList.remove('hidden');
    document.getElementById('reschedule-shift-search').setAttribute('aria-expanded', 'true');
}

document.getElementById('reschedule-shift-search').addEventListener('focus', openShiftList);
document.getElementById('reschedule-shift-search').addEventListener('click', openShiftList);

document.getElementById('reschedule-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (rescheduleState.selectedId == null) return;

    const btn = document.getElementById('reschedule-confirm-btn');
    btn.disabled = true;

    try {
        await api(`/bookings/${rescheduleState.booking.id}/reschedule`, {
            method: 'POST',
            body: JSON.stringify({ shiftId: rescheduleState.selectedId }),
        });
        document.getElementById('reschedule-dialog').close();
        notify('Booking rescheduled');
        loadBookings();
    } catch (err) {
        notify(err.message, 'error');
        btn.disabled = false;
    }
});

document.getElementById('reschedule-dialog').addEventListener('close', () => {
    rescheduleState.booking = null;
});

document.getElementById('booking-courier-filter').addEventListener('change', loadBookings);
document.getElementById('booking-status-filter').addEventListener('change', loadBookings);

// ---------- init ----------
const loaders = { shifts: loadShifts, couriers: loadCouriers, bookings: loadBookings };

(async function init() {
    try {
        state.meta = await api('/meta');
        fillSelect(document.getElementById('shift-zone-filter'), state.meta.zones, { placeholder: 'All zones' });
        fillSelect(document.getElementById('shift-zone'), state.meta.zones);
        fillSelect(document.getElementById('courier-vehicle'), state.meta.vehicleTypes);
    } catch (e) {
        notify('Could not reach the API: ' + e.message, 'error');
    }
    loadShifts();
})();
