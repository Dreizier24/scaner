#!/usr/bin/env bash
# One-time: put the CI keystore and SSH key into GitHub Actions secrets.
# Needs a PAT with repo scope in GH_TOKEN (not the deploy key).
set -euo pipefail
if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "Export GH_TOKEN (GitHub PAT, scope repo) and run again." >&2
  exit 1
fi
command -v gh >/dev/null || { echo "Install gh: apt-get install -y gh" >&2; exit 1; }
REPO="${GITHUB_REPO:-Dreizier24/scaner}"
KEYSTORE="${KEYSTORE_PATH:-/root/.scaner-secrets/debug.keystore}"
SSH_KEY="${SSH_KEY_PATH:-/root/.scaner-secrets/github_actions_ed25519}"
[[ -f "$KEYSTORE" && -f "$SSH_KEY" ]] || { echo "Missing keystore or ssh key" >&2; exit 1; }
base64 -w0 "$KEYSTORE" | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO"
gh secret set SCANER_SSH_KEY --repo "$REPO" < "$SSH_KEY"
echo "Secrets set on $REPO: ANDROID_KEYSTORE_BASE64, SCANER_SSH_KEY"
