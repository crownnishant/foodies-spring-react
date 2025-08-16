import { createContext, useEffect, useRef, useState } from "react";
import { fetchFoodList } from "../service/foodService";
import { getCart, saveCart, removeCartItem, removeCartItemLegacy } from "../service/cartService";

export const StoreContext = createContext(null);

export const StoreContextProvider = (props) => {
  const [foodList, setFoodList] = useState([]);
  const [quantities, setQuantities] = useState({});

  // ✅ Hydrate token from localStorage immediately
  const [token, setToken] = useState(() => localStorage.getItem("token") || "");

  // Flags
  const fetchedMenuRef = useRef(false);   // fetched menu once
  const hydratedRef = useRef(false);      // cart loaded from server once

  // Keep localStorage in sync with token
  useEffect(() => {
    if (token) localStorage.setItem("token", token);
    else localStorage.removeItem("token");
  }, [token]);

  const increaseQty = (foodId) => {
    const id = Number(foodId);
    setQuantities((prev) => ({ ...prev, [id]: (prev[id] || 0) + 1 }));
  };

  const decreaseQty = (foodId) => {
    const id = Number(foodId);
    setQuantities((prev) => {
      const cur = prev[id] || 0;
      const next = { ...prev };
      if (cur <= 1) delete next[id];
      else next[id] = cur - 1;
      return next;
    });
  };

  // ✅ remove item from backend + local state
  const removeFromCart = async (foodId) => {
    const id = Number(foodId);
    try {
      try {
        // Preferred REST style
        await removeCartItem(id, token);
      } catch (e) {
        // Fallback if backend uses legacy endpoint
        await removeCartItemLegacy(id, token);
      }
      setQuantities((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    } catch (err) {
      console.error("Failed to remove item:", err?.response?.data || err.message);
    }
  };

  // ✅ Load cart from server for a given token
  const loadCartData = async (jwtToken) => {
    if (!jwtToken) return;
    try {
      const data = await getCart(jwtToken);
      const items = data?.items || {};
      const normalized = Object.fromEntries(
        Object.entries(items).map(([k, v]) => [Number(k), v])
      );
      setQuantities(normalized);
      hydratedRef.current = true; // enable save effect after initial load
    } catch (err) {
      console.error("Failed to load cart:", err?.response?.data || err.message);
    }
  };

  // Fetch menu once
  useEffect(() => {
    if (fetchedMenuRef.current) return;
    fetchedMenuRef.current = true;
    (async () => {
      try {
        const data = await fetchFoodList();
        setFoodList(data);
      } catch (err) {
        console.error("Failed to fetch menu:", err?.response?.data || err.message);
      }
    })();
  }, []);

  // Load cart once when a token is present
  useEffect(() => {
    if (!token) return;
    if (hydratedRef.current) return; // load only once per session
    (async () => {
      await loadCartData(token);
    })();
  }, [token]);

  // Debounced save to backend when cart changes (after hydration)
  useEffect(() => {
    if (!hydratedRef.current) return;
    if (!token) return; // nothing to save without auth

    const t = setTimeout(async () => {
      try {
        await saveCart(quantities, token);
      } catch (err) {
        console.error(
          "Cart save failed:",
          err?.response?.status,
          err?.response?.data || err?.message
        );
      }
    }, 300);

    return () => clearTimeout(t);
  }, [quantities, token]);

  const contextValue = {
    foodList,
    quantities,
    increaseQty,
    decreaseQty,
    removeFromCart, // calls backend + updates state
    token,
    setToken,
    loadCartData,
    setQuantities,
  };

  return (
    <StoreContext.Provider value={contextValue}>
      {props.children}
    </StoreContext.Provider>
  );
};
