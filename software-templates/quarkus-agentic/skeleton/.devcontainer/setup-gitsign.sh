#!/usr/bin/env bash
set -euo pipefail
echo "============================================================"
echo "[SSSC] Dev Spaces: Sigstore commit signing"
echo "gitsign uses the same Fulcio + Rekor trust root as cosign."
echo "============================================================"
mkdir -p "${HOME}/.local/bin"
export PATH="${HOME}/.local/bin:${PATH}"
if ! command -v gitsign >/dev/null 2>&1; then
  echo "Installing gitsign into ~/.local/bin ..."
  curl -fsSL -o "${HOME}/.local/bin/gitsign" \
    "https://github.com/sigstore/gitsign/releases/download/v0.13.0/gitsign_0.13.0_linux_amd64"
  chmod +x "${HOME}/.local/bin/gitsign"
fi
git config --global --add safe.directory "*" || true
git config --global gpg.x509.program gitsign
git config --global gpg.format x509
git config --global commit.gpgsign true
git config --global tag.gpgsign true
git config --global gitsign.fulcio "${GITSIGN_FULCIO_URL}"
git config --global gitsign.rekor "${GITSIGN_REKOR_URL}"
git config --global gitsign.issuer "${GITSIGN_OIDC_ISSUER}"
git config --global gitsign.clientid "${GITSIGN_OIDC_CLIENT_ID:-trusted-artifact-signer}"
if ! grep -q '.local/bin' "${HOME}/.bashrc" 2>/dev/null; then
  echo 'export PATH="${HOME}/.local/bin:${PATH}"' >> "${HOME}/.bashrc"
fi
echo "gitsign=$(command -v gitsign)"
echo "Fulcio: ${GITSIGN_FULCIO_URL}"
echo "Rekor:  ${GITSIGN_REKOR_URL}"
echo "Issuer: ${GITSIGN_OIDC_ISSUER}"
echo
echo "PASS: git commit will Sigstore-sign (OIDC login in the browser on first commit)."
echo "The pipeline task gitsign-verify checks this signature before OpenShift Builds."
