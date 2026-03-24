import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class NotificationService {
  private readonly logger = new Logger(NotificationService.name);

  constructor(private config: ConfigService) {}

  async sendTelegram(message: string): Promise<void> {
    const token = this.config.get<string>('TELEGRAM_BOT_TOKEN');
    const chatId = this.config.get<string>('TELEGRAM_CHAT_ID');

    if (!token || !chatId) {
      this.logger.warn('Telegram não configurado (TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID ausentes)');
      return;
    }

    try {
      const url = `https://api.telegram.org/bot${token}/sendMessage`;
      const body = JSON.stringify({
        chat_id: chatId,
        text: message,
        parse_mode: 'HTML',
      });

      await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
      });
    } catch (e) {
      this.logger.error('Falha ao enviar notificação Telegram', e);
    }
  }

  async notifySessionExpired(condominioId?: string): Promise<void> {
    const now = new Date().toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' });
    const local = condominioId ? `\nEstoque: <code>${condominioId}</code>` : '';

    await this.sendTelegram(
      `⚠️ <b>Market Kiosk — Sessão Expirada</b>\n\n` +
      `A sessão do quiosque expirou e nenhum token pôde ser renovado.\n` +
      `${local}\n` +
      `🕐 ${now}\n\n` +
      `<b>Acesse o tablet e faça login novamente.</b>`
    );
  }

  async notifyTokenExpiringSoon(daysLeft: number, userCount: number): Promise<void> {
    const now = new Date().toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' });

    let emoji = '🟡';
    let urgency = '';

    if (daysLeft === 3) {
      emoji = '🟡';
      urgency = 'A sessão expira em <b>3 dias</b>. Você ainda tem tempo.';
    } else if (daysLeft === 2) {
      emoji = '🟠';
      urgency = 'A sessão expira em <b>2 dias</b>. Prepare-se para fazer login em breve.';
    } else if (daysLeft === 1) {
      emoji = '🔴';
      urgency = '⚡ A sessão expira <b>AMANHÃ</b>! Faça login no tablet hoje para renovar.';
    }

    const sessoes = userCount === 1 ? '1 sessão' : `${userCount} sessões`;

    await this.sendTelegram(
      `${emoji} <b>Market Kiosk — Aviso de Expiração</b>\n\n` +
      `${urgency}\n\n` +
      `📱 ${sessoes} ativa(s) no sistema\n` +
      `🕐 ${now}\n\n` +
      `<i>Abra o app e use normalmente para renovar automaticamente.</i>`
    );
  }
}
