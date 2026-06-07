package com.smartinventory.enums;

/**
 * Classifies a stock movement. Each type carries a {@code direction}:
 * <ul>
 *   <li>{@code +1} increases on-hand stock (STOCK_IN, RETURN)</li>
 *   <li>{@code -1} decreases on-hand stock (STOCK_OUT)</li>
 *   <li>{@code 0} ADJUSTMENT sets the quantity delta explicitly (can be + or -)</li>
 * </ul>
 */
public enum TransactionType {
    STOCK_IN(1),
    STOCK_OUT(-1),
    RETURN(1),
    ADJUSTMENT(0);

    private final int direction;

    TransactionType(int direction) {
        this.direction = direction;
    }

    /** @return +1, -1, or 0 (adjustment supplies its own signed quantity). */
    public int getDirection() {
        return direction;
    }
}
