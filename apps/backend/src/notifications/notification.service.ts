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
}
