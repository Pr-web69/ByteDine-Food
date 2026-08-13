import request from '@/utils/request'

export const getCartList = () =>
    request.get('/user/cart/list')

export const addToCart = (dishId, quantity = 1, price, specInfo) =>
    request.post('/user/cart', { dishId, quantity, price, specInfo })

export const removeFromCart = (dishId, quantity = 1) =>
    request.put('/user/cart/'.concat(dishId, '/reduce?quantity=', quantity))

export const updateCartQty = (cartId, quantity) =>
    request.put('/user/cart/'.concat(cartId, '?quantity=', quantity))

export const deleteCartItem = (cartId) =>
    request.delete('/user/cart/'.concat(cartId))

export const clearCart = () =>
    request.delete('/user/cart/clear')
