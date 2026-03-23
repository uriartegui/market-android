import {
  Controller, Get, Post, Put, Delete, Patch,
  Body, Param, Query, UseGuards, Request,
} from '@nestjs/common';
import { ProductsService } from './products.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { AdjustStockDto } from './dto/adjust-stock.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('products')
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Post()
  create(@Body() dto: CreateProductDto, @Request() req) {
    return this.productsService.create(dto, req.user.condominioId);
  }

  @Get()
  findAll(
    @Request() req,
    @Query('category') category?: string,
    @Query('search') search?: string,
    @Query('lowStock') lowStock?: string,
  ) {
    return this.productsService.findAll(
      req.user.condominioId,
      category,
      search,
      lowStock === 'true',
    );
  }

  @Get('scan/:qrCode')
  findByQrCode(@Param('qrCode') qrCode: string, @Request() req) {
    return this.productsService.findByQrCode(qrCode, req.user.condominioId);
  }

  @Get('barcode/:barcode')
  findByBarcode(@Param('barcode') barcode: string, @Request() req) {
    return this.productsService.findByBarcode(barcode, req.user.condominioId);
  }

  @Get(':id')
  findOne(@Param('id') id: string, @Request() req) {
    return this.productsService.findOne(id, req.user.condominioId);
  }

  @Put(':id')
  update(@Param('id') id: string, @Body() dto: UpdateProductDto, @Request() req) {
    return this.productsService.update(id, dto, req.user.condominioId);
  }

  @Patch(':id/stock')
  adjustStock(@Param('id') id: string, @Body() dto: AdjustStockDto, @Request() req) {
    return this.productsService.adjustStock(id, dto, req.user.condominioId);
  }

  @Delete(':id')
  remove(@Param('id') id: string, @Request() req) {
    return this.productsService.remove(id, req.user.condominioId);
  }
}
