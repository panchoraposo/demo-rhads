#!/usr/bin/env bash
# Bootstrap a Python venv, install Ansible collections, then run the RHADS demo installer.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${ROOT}"

if ! command -v oc >/dev/null 2>&1; then
  echo "ERROR: oc CLI is required and must be logged in (context devopsdays)." >&2
  exit 1
fi

if [[ "$(oc config current-context 2>/dev/null || true)" != "devopsdays" ]]; then
  echo "Switching kube context to devopsdays..."
  oc config use-context devopsdays
fi

if ! command -v helm >/dev/null 2>&1; then
  echo "Installing Helm 3..."
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
fi

python3 -m venv "${ROOT}/venv"
# shellcheck disable=SC1091
source "${ROOT}/venv/bin/activate"

pip install --upgrade pip
pip install -r ansible/requirements.txt
ansible-galaxy collection install -r ansible/collections/requirements.yml

exec ansible-playbook -i ansible/inventory ansible/playbooks/install.yaml "$@"
