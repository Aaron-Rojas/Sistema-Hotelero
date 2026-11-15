#!/usr/bin/env bash
# ==============================================================================
# Script de Aprovisionamiento y Hardening para Host Ubuntu 22.04 / 24.04 LTS
# Rol: Lead Cloud & DevOps Engineer
# Destino: Máquina Virtual Linux / Instancia AWS EC2
# ==============================================================================

set -euo pipefail

echo "===================================================================="
echo " Iniciando Aprovisionamiento de Infraestructura en Host Linux"
echo "===================================================================="

# ------------------------------------------------------------------------------
# 1. Actualización de Paquetes Base del Sistema
# ------------------------------------------------------------------------------
echo "[1/5] Actualizando repositorios y paquetes del sistema..."
sudo apt-get update -y
sudo apt-get upgrade -y
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    ufw \
    jq \
    git \
    htop

# ------------------------------------------------------------------------------
# 2. Instalación de Docker Engine y Docker Compose v2 Plugin Oficial
# ¿POR QUÉ repositorio oficial?
# Evita versiones desactualizadas de los repositorios predeterminados de Ubuntu,
# garantizando compatibilidad con compose v2 (docker compose sin guion).
# ------------------------------------------------------------------------------
echo "[2/5] Instalando Docker Engine y Docker Compose v2..."
if ! command -v docker &> /dev/null; then
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl enable --now docker
    echo "Docker instalado exitosamente."
else
    echo "Docker ya se encuentra instalado en el sistema."
fi

# Configuración de usuario no-root para operaciones Docker
if id "ubuntu" &>/dev/null; then
    sudo usermod -aG docker ubuntu
    echo "Usuario 'ubuntu' añadido al grupo 'docker'."
fi

# ------------------------------------------------------------------------------
# 3. Optimización del Kernel de Linux (Kernel Tuning)
# ¿POR QUÉ vm.overcommit_memory = 1?
# Redis requiere asignación de memoria sin fallos en background saving (BGSAVE).
# ¿POR QUÉ somaxconn = 65535?
# Eleva el límite de conexiones TCP en cola para soportar picos altos de concurrencia.
# ------------------------------------------------------------------------------
echo "[3/5] Aplicando optimizaciones de Kernel (sysctl para Redis y Alta Concurrencia)..."
cat << 'EOF' | sudo tee /etc/sysctl.d/99-cloud-performance.conf
# Optimización para Redis BGSAVE sin degradación
vm.overcommit_memory = 1

# Límite máximo de sockets TCP en cola de espera (concurrencia masiva)
net.core.somaxconn = 65535

# Protección contra ataques de SYN Flood
net.ipv4.tcp_syncookies = 1
EOF

sudo sysctl --system

# Desactivar Transparent Huge Pages (THP) para prevenir latencias en Redis
if test -f /sys/kernel/mm/transparent_hugepage/enabled; then
    echo never | sudo tee /sys/kernel/mm/transparent_hugepage/enabled
fi

# ------------------------------------------------------------------------------
# 4. Hardening de Seguridad de Red (Firewall UFW)
# ¿POR QUÉ denegar por defecto?
# Principio de defensa en profundidad: solo se exponen los puertos indispensables.
# ------------------------------------------------------------------------------
echo "[4/5] Configurando Firewall UFW (Mínimo Privilegio)..."
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH Seguro
sudo ufw allow 22/tcp comment "SSH Remote Administration"

# Tráfico Web hacia Nginx Frontend & Reverse Proxy
sudo ufw allow 80/tcp comment "HTTP Frontend & Ingress"
sudo ufw allow 443/tcp comment "HTTPS TLS Termination"

# Puerto de Observabilidad (Grafana)
sudo ufw allow 3001/tcp comment "Grafana Telemetry Dashboard"

# Habilitar firewall sin confirmación interactiva
sudo ufw --force enable
sudo ufw status verbose

# ------------------------------------------------------------------------------
# 5. Verificación Final
# ------------------------------------------------------------------------------
echo "[5/5] Verificando instalación..."
docker --version
docker compose version

echo "===================================================================="
echo " Aprovisionamiento completado con éxito."
echo " Reinicia tu sesión de terminal para aplicar los permisos de Docker."
echo "===================================================================="

