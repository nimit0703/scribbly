import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";
import router from "./router";

// Initialize Feather Icons after DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  if (typeof feather !== "undefined") {
    feather.replace();
  }
});
createApp(App).use(router).mount("#app");
