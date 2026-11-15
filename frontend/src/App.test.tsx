import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

describe("App", () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows the backend as up when the health endpoint answers", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("{}", { status: 200 }));

    render(<App />);

    expect(await screen.findByText("up")).toBeInTheDocument();
  });

  it("shows the backend as down when the request fails", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("network"));

    render(<App />);

    expect(await screen.findByText("down")).toBeInTheDocument();
  });
});
