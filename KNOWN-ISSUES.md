# Known Issues — SKB-Player

Priority: 🔴 critical · 🟠 high · 🟡 medium · 🟢 low

---

## 🟠 KI-P01 · Version drift (file vs commits)

**Reality:** `build.gradle.kts` = `versionName "0.1.0"`,
`versionCode 1`. Commit history reaches v2.4.

**Impact:** APK says "0.1.0" — misleading.

**Fix:** Bump to `versionName "2.4.0"`, `versionCode 24`.

---

## 🟠 KI-P02 · `.gitignore` ignores `.github/` but it's tracked

**Reality:** `.gitignore` line `.github/` — but workflow file exists.

**Impact:** Confusing. New workflow files would be silently ignored.

**Fix:** Remove `.github/` line from `.gitignore`.

---

## 🟠 KI-P03 · No README / CHANGELOG

**Reality:** Repo has neither.

**Impact:** New users/AI have zero context.

**Fix:** Add README.md, CHANGELOG.md (this release).

---

## 🟡 KI-P04 · 3 dialogs files (consolidation candidate)

**Reality:** `BookmarkDialogs.kt`, `VideoDialogs.kt`,
`NewDialogs.kt`.

**Impact:** Related UI split across files.

**Fix:** Merge into `Dialogs.kt` or organize by domain.

---

## 🟡 KI-P05 · No test suite

**Reality:** Zero tests.

**Impact:** Regressions on refactor.

**Fix:** Start with managers unit tests.

---

## 🟡 KI-P06 · Reflection for Media3 setUseTextureView

**Reality:** Reflection workaround (commit e9d1519).

**Impact:** Fragile, breaks if Media3 internal names change.

**Fix:** Watch upstream, remove when fixed.

---

## 🟢 KI-P07 · No Room / proper DB

**Reality:** Managers use SharedPreferences / JSON? Unclear.

**Impact:** Scale limits (thousands of videos).

**Fix:** Migrate to Room (v3.0).

---

## 🟢 KI-P08 · No LICENSE

**Reality:** No license file.

**Fix:** Add Apache-2.0 or MIT (SKB choice).

---

## Resolved (this release)

_(none yet)_
