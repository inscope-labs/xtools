/**
 * script.js - SQLite Database CRUD Plugin Controller
 */
(function() {
    'use strict';

    // DOM Elements
    var dbStatusEl = document.getElementById('dbStatus');
    var btnInitDb = document.getElementById('btnInitDb');
    var btnRefresh = document.getElementById('btnRefresh');
    var btnDropDb = document.getElementById('btnDropDb');

    var formTitleEl = document.getElementById('formTitle');
    var recordIdEl = document.getElementById('recordId');
    var inputNameEl = document.getElementById('inputName');
    var inputEmailEl = document.getElementById('inputEmail');
    var inputRoleEl = document.getElementById('inputRole');

    var btnSaveRecord = document.getElementById('btnSaveRecord');
    var btnCancelEdit = document.getElementById('btnCancelEdit');

    var tableContainer = document.getElementById('tableContainer');
    var sqlQueryEl = document.getElementById('sqlQuery');
    var btnExecuteSql = document.getElementById('btnExecuteSql');
    var sqlOutputEl = document.getElementById('sqlOutput');

    var isInitialized = false;

    // Helper functions for UI feedback
    function showToast(msg) {
        if (window.XTools && window.XTools.ui && window.XTools.ui.showToast) {
            window.XTools.ui.showToast(msg);
        } else {
            console.log('[Toast]', msg);
        }
    }

    function triggerVibrate() {
        if (window.XTools && window.XTools.ui && window.XTools.ui.vibrate) {
            window.XTools.ui.vibrate(50);
        }
    }

    function logOutput(data) {
        if (typeof data === 'object') {
            sqlOutputEl.textContent = JSON.stringify(data, null, 2);
        } else {
            sqlOutputEl.textContent = String(data);
        }
    }

    // Initialize Database Table
    async function initDatabase() {
        var createSql = "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "email TEXT NOT NULL, " +
            "role TEXT NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");";

        try {
            if (window.XTools && window.XTools.database) {
                await window.XTools.database.execute(createSql);
            }
            isInitialized = true;
            dbStatusEl.textContent = 'Ready (SQLite)';
            dbStatusEl.className = 'status-badge online';
            showToast('SQLite table initialized successfully');
            triggerVibrate();
            loadRecords();
        } catch (err) {
            console.error('Database Init Error:', err);
            dbStatusEl.textContent = 'Error';
            logOutput('Error initializing database: ' + err.message);
        }
    }

    // Load / Query All Records
    async function loadRecords() {
        var querySql = "SELECT * FROM users ORDER BY id DESC;";
        try {
            var rows = [];
            if (window.XTools && window.XTools.database) {
                rows = await window.XTools.database.query(querySql);
            }

            renderTable(rows || []);
            logOutput(rows);
        } catch (err) {
            console.error('Load Records Error:', err);
            // If table doesn't exist yet, offer to init
            tableContainer.innerHTML = '<div class="empty-state">Table not initialized or empty. Click "Initialize Table".</div>';
            logOutput('Error querying records: ' + err.message);
        }
    }

    // Render Records Table
    function renderTable(rows) {
        if (!rows || rows.length === 0) {
            tableContainer.innerHTML = '<div class="empty-state">No records found. Fill the form above to insert data.</div>';
            return;
        }

        var html = '<table><thead><tr>' +
            '<th>ID</th>' +
            '<th>Name</th>' +
            '<th>Email</th>' +
            '<th>Role</th>' +
            '<th>Actions</th>' +
            '</tr></thead><tbody>';

        rows.forEach(function(row) {
            var rowId = row.id;
            var nameEsc = escapeHtml(row.name);
            var emailEsc = escapeHtml(row.email);
            var roleEsc = escapeHtml(row.role);

            html += '<tr>' +
                '<td>' + rowId + '</td>' +
                '<td><strong>' + nameEsc + '</strong></td>' +
                '<td>' + emailEsc + '</td>' +
                '<td><span class="status-badge">' + roleEsc + '</span></td>' +
                '<td>' +
                '<button class="action-btn action-edit" onclick="editRecord(' + rowId + ', \'' + escapeJs(row.name) + '\', \'' + escapeJs(row.email) + '\', \'' + escapeJs(row.role) + '\')">Edit</button>' +
                '<button class="action-btn action-delete" onclick="deleteRecord(' + rowId + ')">Delete</button>' +
                '</td>' +
                '</tr>';
        });

        html += '</tbody></table>';
        tableContainer.innerHTML = html;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function escapeJs(str) {
        if (!str) return '';
        return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
    }

    // Insert or Update Record
    async function saveRecord() {
        var id = recordIdEl.value;
        var name = inputNameEl.value.trim();
        var email = inputEmailEl.value.trim();
        var role = inputRoleEl.value;

        if (!name || !email) {
            showToast('Please enter both name and email');
            return;
        }

        try {
            if (id) {
                // UPDATE Record
                var updateSql = "UPDATE users SET name = '" + sqlSanitize(name) + "', email = '" + sqlSanitize(email) + "', role = '" + sqlSanitize(role) + "' WHERE id = " + parseInt(id, 10) + ";";
                await window.XTools.database.execute(updateSql);
                showToast('Record #' + id + ' updated');
            } else {
                // INSERT Record
                var insertSql = "INSERT INTO users (name, email, role) VALUES ('" + sqlSanitize(name) + "', '" + sqlSanitize(email) + "', '" + sqlSanitize(role) + "');";
                await window.XTools.database.execute(insertSql);
                showToast('New user record inserted');
            }

            triggerVibrate();
            resetForm();
            loadRecords();
        } catch (err) {
            console.error('Save Record Error:', err);
            logOutput('Error saving record: ' + err.message);
        }
    }

    function sqlSanitize(str) {
        return str.replace(/'/g, "''");
    }

    // Delete Record
    window.deleteRecord = async function(id) {
        var deleteSql = "DELETE FROM users WHERE id = " + parseInt(id, 10) + ";";
        try {
            await window.XTools.database.execute(deleteSql);
            showToast('Deleted record #' + id);
            triggerVibrate();
            loadRecords();
        } catch (err) {
            console.error('Delete Error:', err);
            logOutput('Error deleting record: ' + err.message);
        }
    };

    // Edit Record
    window.editRecord = function(id, name, email, role) {
        recordIdEl.value = id;
        inputNameEl.value = name;
        inputEmailEl.value = email;
        inputRoleEl.value = role;

        formTitleEl.textContent = 'Edit Record #' + id;
        btnSaveRecord.textContent = 'Update Record';
        btnCancelEdit.style.display = 'inline-flex';
        inputNameEl.focus();
    };

    function resetForm() {
        recordIdEl.value = '';
        inputNameEl.value = '';
        inputEmailEl.value = '';
        inputRoleEl.value = 'Developer';

        formTitleEl.textContent = 'Add New Record';
        btnSaveRecord.textContent = 'Save Record';
        btnCancelEdit.style.display = 'none';
    }

    // Drop Table
    async function dropTable() {
        var dropSql = "DROP TABLE IF EXISTS users;";
        try {
            await window.XTools.database.execute(dropSql);
            showToast('Database table dropped');
            triggerVibrate();
            resetForm();
            loadRecords();
        } catch (err) {
            console.error('Drop Error:', err);
            logOutput('Error dropping table: ' + err.message);
        }
    }

    // Custom SQL Execution
    async function executeCustomSql() {
        var sql = sqlQueryEl.value.trim();
        if (!sql) {
            showToast('Enter a SQL query');
            return;
        }

        try {
            if (sql.toLowerCase().startsWith('select')) {
                var results = await window.XTools.database.query(sql);
                logOutput(results);
                showToast('Query executed successfully');
            } else {
                await window.XTools.database.execute(sql);
                logOutput({ success: true, message: 'Statement executed successfully' });
                showToast('SQL Statement Executed');
                loadRecords();
            }
            triggerVibrate();
        } catch (err) {
            logOutput('SQL Execution Error: ' + err.message);
        }
    }

    // Event Listeners
    btnInitDb.addEventListener('click', initDatabase);
    btnRefresh.addEventListener('click', loadRecords);
    btnDropDb.addEventListener('click', dropTable);
    btnSaveRecord.addEventListener('click', saveRecord);
    btnCancelEdit.addEventListener('click', resetForm);
    btnExecuteSql.addEventListener('click', executeCustomSql);

    // Initial load
    window.addEventListener('DOMContentLoaded', function() {
        initDatabase();
    });
})();
