// src/service/cartService.js
import axios from "axios";

const API_URL = "http://localhost:8080/api/v1/cart";

const authHeaders = (token) => ({
  headers: { Authorization: `Bearer ${token}` }
});

/** GET /api/v1/cart  -> { id, userId, items: {"23":1, ...} } */
export const getCart = async (token) => {
  if (!token) return { items: {} };
  const res = await axios.get(API_URL, authHeaders(token));
  return res.data;
};

/** PUT /api/v1/cart/save  body: { items } */
export const saveCart = async (items, token) => {
  if (!token) return;
  await axios.put(
    `${API_URL}/save`,
    { items },
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`
      }
    }
  );
};

/**
 * REMOVE one item from cart (server-side).
 * Preferred: DELETE /api/v1/cart/remove/{foodId}
 */
export const removeCartItem = async (foodId, token) => {
  if (!token) return;
  await axios.delete(`${API_URL}/remove/${foodId}`, authHeaders(token));
};

/** Legacy fallback if your backend expects a POST body: { foodId } */
export const removeCartItemLegacy = async (foodId, token) => {
  if (!token) return;
  await axios.post(
    `${API_URL}/remove`,
    { foodId },
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`
      }
    }
  );
};