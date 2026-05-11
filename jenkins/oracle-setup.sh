#!/usr/bin/env bash
# =============================================================================
# Oracle Cloud Free Tier VM — First-time setup script
# Tested on: Ubuntu 22.04 (Ampere A1 ARM64 and AMD x86_64)
#
# Run as root:
#   sudo bash oracle-setup.sh
# =============================================================================
set -euo pipefail

echo "========================================"
echo " Oracle Cloud VM — DevOps setup"
echo "========================================"

# ── 1. System update ─────────────────────────────────────────────────────────
echo "[1/6] Updating system packages..."
apt-get update -qq
apt-get upgrade -y -qq

# ── 2. Install Docker ─────────────────────────────────────────────────────────
echo "[2/6] Installing Docker..."
apt-get install -y -qq ca-certificates curl gnupg

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -qq
apt-get install -y docker-ce docker-ce-cli containerd.io \
                   docker-buildx-plugin docker-compose-plugin

# Enable and start Docker
systemctl enable --now docker

echo "Docker version: $(docker --version)"
echo "Docker Compose version: $(docker compose version)"

# ── 3. Install Certbot ────────────────────────────────────────────────────────
echo "[3/6] Installing Certbot..."
apt-get install -y -qq snapd
snap install core
snap refresh core
snap install --classic certbot
ln -sf /snap/bin/certbot /usr/bin/certbot

# ── 4. Create application directories ────────────────────────────────────────
echo "[4/6] Creating app directories..."
mkdir -p /opt/coupon-service
mkdir -p /opt/jenkins
mkdir -p /var/www/certbot

# ── 5. Open ports in Oracle Cloud iptables ────────────────────────────────────
# Oracle Cloud VMs have host-level iptables rules that block traffic by default,
# even if Security Lists are open. These rules must be added manually.
echo "[5/6] Opening ports in iptables..."

# HTTP and HTTPS (for nginx + certbot)
iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80  -j ACCEPT
iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT

# Jenkins JNLP agent port (optional — only needed for remote build agents)
iptables -I INPUT 6 -m state --state NEW -p tcp --dport 50000 -j ACCEPT

# Persist across reboots
apt-get install -y -qq iptables-persistent
netfilter-persistent save

# ── 6. Harden SSH ─────────────────────────────────────────────────────────────
echo "[6/6] Hardening SSH (disable password auth)..."
sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
systemctl reload sshd

# ── Done ──────────────────────────────────────────────────────────────────────
cat << 'EOF'

========================================
 Setup complete — next steps:
========================================

1. Copy the jenkins/ directory from the repo to /opt/jenkins/ on this VM:
     scp -r jenkins/ ubuntu@YOUR_VM_IP:/opt/jenkins/

2. Edit /opt/jenkins/nginx.conf — replace YOUR_DOMAIN with your domain.

3. Start Jenkins and nginx:
     cd /opt/jenkins
     docker compose up -d

4. Issue a TLS certificate (requires DNS pointing to this VM):
     certbot --nginx -d YOUR_DOMAIN

5. Get the Jenkins initial admin password:
     docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

6. Open https://YOUR_DOMAIN in your browser and complete the setup wizard.

7. In Jenkins → Manage Jenkins → Credentials, add:
     - ghcr-token    (Secret text)    — GitHub PAT with packages:read/write
     - oracle-vm-ssh (SSH private key) — private key for deploying to this VM

8. In Jenkins → Manage Jenkins → System, add global env vars:
     - ORACLE_VM_HOST  = <this VM's public IP>
     - GITHUB_REPO     = your-org/coupon-service

9. Create a new Pipeline job pointing to your GitHub repo.
   Jenkins will detect the Jenkinsfile automatically.

EOF
